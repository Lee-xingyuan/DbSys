import {get,write} from "./api.js";
import {el,box,button,link,form,pager,table,states,orderStates,session,login,url,go,day,time,period,serverClock,refresh,notice} from "./ui.js";

function pendingKey(action,id) { return ["cinema",session.user.userId,action,id].join("."); }
function pending(key) { return JSON.parse(sessionStorage.getItem(key) || "null"); }
function saveAttempt(key,payload) {
  const attempt={...payload,requestKey:crypto.randomUUID()};
  sessionStorage.setItem(key,JSON.stringify(attempt));
  return attempt;
}
async function transmit(key,path,payload) {
  try {
    const result=await write(path,"POST",payload);
    sessionStorage.removeItem(key);
    return result;
  } catch (e) {
    // 连接中断、5xx 或可重试冲突可能尚未确定结果。恢复时沿用原内容。
    if ([400,404,409].includes(e.status) && e.code !== "RETRY_REQUIRED") sessionStorage.removeItem(key);
    if (e.status === 409) refresh();
    throw e;
  }
}
export async function seats(view,q,id) {
  const {movieId,...filters}=q;
  const back=url("movie/"+movieId,filters);
  const [map,shows]=await Promise.all([
    get("/screenings/"+id+"/seats"),
    get("/movies/"+q.movieId+"/screenings",{date:q.date || day(),cinemaId:q.cinemaId,page:q.page || 1,pageSize:10})
  ]);
  const show=shows.items.find(s=>String(s.screeningId)===id);
  if (!show || show.revision!==map.revision) {
    view.append(link("返回并刷新场次",back),el("p","场次信息已变更，请重新核对影厅、时间和票价。"),link("核对已有订单","orders"));
    return;
  }
  const now=serverClock(map.serverTime);
  const stop=Date.parse(map.salesStopAt);
  view.append(link("返回场次",back),el("h1","选择座位"));
  if (show) view.append(el("p",show.movieTitle+" · "+show.cinemaName+" · "+show.hallName+" · "+period(show.startsAt,show.endsAt)));
  view.append(el("p","场次 #"+id+" · 单座 ¥"+map.ticketPrice+" · 最多选 4 座"),box("screen","银幕方向"));
  const selected=new Map();
  const grid=box("scroll"), summary=box("panel"), actions=box("actions");
  const groups=new Map();
  const labels={AVAILABLE:"可选",HELD:"占用",SOLD:"已售",DISABLED:"禁用"};
  function draw() {
    summary.textContent=(selected.size ? [...selected.values()].join("、") : "尚未选座")+" · 合计 ¥"+(Math.round(Number(map.ticketPrice)*100)*selected.size/100).toFixed(2);
  }
  for (const seat of map.seats) {
    if (!groups.has(seat.rowNumber)) groups.set(seat.rowNumber,box("seatrow",el("strong",seat.rowNumber+" 排")));
    const b=el("button");
    b.type="button"; b.className="seat";
    const name=seat.rowNumber+" 排 "+seat.seatNumber+" 座";
    function paint() {
      const active=selected.has(seat.screeningSeatId);
      const label=active ? "已选" : labels[seat.inventoryStatus];
      b.textContent=seat.seatNumber+" "+label;
      b.setAttribute("aria-label",name+" "+label);
      b.setAttribute("aria-pressed",String(active));
    }
    b.disabled=seat.inventoryStatus!=="AVAILABLE" || now()>=stop;
    b.onclick=()=>{
      if (selected.has(seat.screeningSeatId)) selected.delete(seat.screeningSeatId);
      else if (selected.size<4) selected.set(seat.screeningSeatId,name);
      else { notice("每单最多选择 4 座。"); return; }
      paint(); draw();
    };
    paint(); groups.get(seat.rowNumber).append(b);
  }
  grid.append(...groups.values());
  const key=session.user && pendingKey("order",id);
  const original=key && pending(key);
  const send=button(original ? "核对并重试上次下单" : "确认选座并下单",async()=>{
    if (!session.user) return login();
    if (!original && !selected.size) throw new Error("请先选择座位。");
    if (!original && now()>=stop) { refresh(); throw new Error("场次已停售，请返回选择其他场次。"); }
    const seatIds=original?.screeningSeatIds || [...selected.keys()];
    const names=seatIds.map(n=>map.seats.find(s=>s.screeningSeatId===n)).map((s,i)=>s ? s.rowNumber+"排"+s.seatNumber+"座" : "座位编号 "+seatIds[i]);
    const details=original ? "核对上次下单结果，将沿用原请求和座位。" :
      "合计 ¥"+(Math.round(Number(map.ticketPrice)*100)*seatIds.length/100).toFixed(2)+"，预计支付截止 "+time(Math.min(now()+600000,stop))+"。"+(now()<stop-25*60000 ? "支付后可在开场前30分钟以前整单退款。" : "现已过退票截止时间，支付后不可退。");
    if (!confirm([show ? show.movieTitle+" · "+show.cinemaName+" · "+show.hallName : "场次 #"+id,names.join("、"),details].join("\n"))) return;
    const attempt=original || saveAttempt(key,{screeningId:map.screeningId,revision:map.revision,screeningSeatIds:seatIds});
    try {
      const order=await transmit(key,"/orders",attempt);
      go("order/"+order.orderId);
    } catch(e) { if (e.status!==401) refresh(); throw e; }
  });
  draw();
  actions.append(send,button("刷新余座",refresh));
  if (original) {
    grid.inert=true;
    summary.textContent="上次下单结果待核对，可用原请求重试或查看我的订单。";
    actions.append(link("我的订单","orders"));
  }
  view.append(grid,summary,actions);
}
export async function orders(view,q) {
  const result=await get("/orders",{status:q.status,page:q.page || 1,pageSize:10});
  view.append(el("h1","我的订单"),form([{name:"status",title:"订单状态",optional:true,value:q.status || "",options:[["","全部状态"],...orderStates]}],
    "筛选",v=>go("orders",v),"filters"));
  view.append(orderTable(result),pager(result));
}
export function orderTable(result,admin=false) {
  return table(["订单","影片 / 影院","开场时间","成交金额","状态"],result.items.map(o=>[
    link("#"+o.orderId,(admin ? "admin-order/" : "order/")+o.orderId),o.movieTitle+" · "+o.cinemaName,time(o.startsAt),"¥"+o.totalAmount,states[o.status]
  ]));
}
export async function order(view,q,id,admin=false) {
  const path="/orders/"+id;
  const o=await get((admin ? "/admin" : "")+path);
  const now=serverClock(o.serverTime);
  const name=o.items.map(i=>i.seatLabel).join("、");
  view.append(link(admin ? "后台订单" : "我的订单",admin ? "admin/orders" : "orders"),el("h1","订单 #"+o.orderId),
    box("panel",el("h3",o.movieTitle+" · "+states[o.status]),el("p",o.cinemaName+" · "+o.hallName),
      el("p",period(o.startsAt,o.endsAt)),el("p",name+" · 合计 ¥"+o.totalAmount),
      el("p","下单 "+time(o.createdAt)+" · 到期 "+time(o.expiresAt)),
      o.paidAt && el("p","支付 "+time(o.paidAt)),o.closedAt && el("p","关闭 "+time(o.closedAt))),
    table(["座位","成交单价"],o.items.map(i=>[i.seatLabel,"¥"+i.ticketPrice])));
  if (o.refund) view.append(box("panel","退款凭证 #"+o.refund.refundId+" · 支付流水 #"+o.refund.paymentAttemptId+" · ¥"+o.refund.refundAmount+" · "+time(o.refund.refundedAt)));
  if (admin) return;
  const key=pendingKey("payment",id);
  const previous=pending(key);
  async function pay(result) {
    const attempt=previous || saveAttempt(key,{simulationResult:result});
    try {
      const res=await transmit(key,path+"/payments",attempt);
      notice(res.payment ? "支付流水 #"+res.payment.paymentAttemptId+" · "+(res.payment.paymentResult==="SUCCESS" ? "模拟支付成功" : "模拟支付失败")+" · "+states[res.order.status] : states[res.order.status]);
    } finally { refresh(); }
  }
  if (previous) view.append(button("用原请求核对上次模拟支付",()=>pay(previous.simulationResult)));
  if (o.status==="PENDING") {
    const countdown=el("p"), terms=el("p"), payControls=box("actions");
    if (!previous) payControls.append(button("模拟支付成功",()=>pay("SUCCESS")),button("模拟支付失败",()=>pay("FAILURE")));
    payControls.append(button("取消订单",async()=>{
      if (confirm("取消整单并释放 "+name+"？")) { await write(path+"/cancel"); refresh(); }
    }));
    // 一次仅允许一个交易操作，包含同栏中的其他按钮。
    view.append(box("panel",el("h3","模拟支付"),countdown,terms,payControls));
    const tick=()=>{
      const left=Math.max(0,Math.ceil((Date.parse(o.expiresAt)-now())/1000));
      countdown.textContent=left ? "剩余支付时间 "+Math.floor(left/60)+" 分 "+left%60+" 秒" : "支付期限已到，请刷新核对订单状态。";
      payControls.hidden=!left;
      terms.textContent=now() < Date.parse(o.refundDeadline) ? "整单退票截止 "+time(o.refundDeadline)+"（北京时间）" : "已过退票截止时间，支付后不可退。";
    };
    tick();
    const timer=setInterval(()=>view.isConnected ? tick() : clearInterval(timer),500);
  }
  if (o.status==="PAID" && o.refundable) view.append(button("申请整单退款",async()=>{
    if (confirm(o.movieTitle+" · "+name+"\n整单退款 ¥"+o.totalAmount+"，确认释放全部座位？")) {
      await write(path+"/refund"); refresh();
    }
  }));
  view.append(button("刷新订单状态",refresh));
}
