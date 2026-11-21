import {all,get,write} from "./api.js";
import {el,box,link,button,form,fold,pager,table,orderStates,states,go,url,refresh,day,time,period,notice,prose} from "./ui.js";
import {orderTable} from "./trade.js";

const sections={movies:"影片资料",cinemas:"影院与影厅",screenings:"排片",orders:"经营订单",reviews:"评论管理",reports:"经营统计",audit:"操作记录"};
const api="/admin/";
const isActive=row=>row.isActive ?? row.active;
const field=(name,title,value="",extra={})=>({name,title,value,...extra});
const number=(name,title,value="",extra={})=>field(name,title,value,{type:"number",min:1,...extra});
const date=(name,title,value)=>field(name,title,value || day(),{type:"date"});
const select=(name,title,options,value="",optional=false)=>field(name,title,value,{options,optional});
function filter(tab,specs) {
  return form(specs,"查询",values=>{
    const start=values.from || values.createdDateFrom;
    const end=values.to || values.createdDateTo;
    if (start && end) {
      const days=(Date.parse(end)-Date.parse(start))/86400000;
      if (days<0 || days>365) throw new Error("请选择先后有序、含首尾最多 366 天的日期范围。");
    }
    go("admin/"+tab,values);
  },"filters");
}
async function save(path,value,method="POST") {
  await write(api+path,method,value);
  notice("操作已保存。");
  refresh();
}
function confirmAction(label,path,payload,method) {
  return button(label,async()=>{ if (confirm("确认"+label+"？")) await save(path,payload,method); });
}
function movieEditor(m={}) {
  return form([
    field("title","片名",m.title,{maxLength:120}),field("genre","类型",m.genre,{maxLength:40}),
    number("durationMinutes","片长（分钟）",m.durationMinutes,{max:32767}),
    field("synopsis","剧情简介",m.synopsis,{type:"textarea",maxLength:10000}),
    field("posterUrl","海报地址",m.posterUrl || "",{optional:true,maxLength:500,placeholder:"/images/… 或 https://…"})
  ],"保存影片",v=>save(m.movieId ? "movies/"+m.movieId : "movies",{...v,durationMinutes:+v.durationMinutes,posterUrl:v.posterUrl || null},m.movieId ? "PUT" : "POST"));
}
async function movies(view,q) {
  const result=await get(api+"movies",{page:q.page || 1});
  view.append(fold("新增影片",movieEditor()));
  for (const m of result.items) view.append(box("panel",el("h3",m.title+" · "+states[m.listingStatus]),el("p","#"+m.movieId+" · "+m.genre+" · "+m.durationMinutes+" 分钟"),
    fold("编辑资料",movieEditor(m)),confirmAction(m.listingStatus==="LISTED" ? "下架影片" : "上架影片","movies/"+m.movieId+"/listing",{listed:m.listingStatus!=="LISTED"})));
  view.append(pager(result));
}
function cinemaEditor(c={}) {
  return form([field("cinemaName","影院名称",c.cinemaName,{maxLength:100}),field("address","地址",c.address,{maxLength:255}),
    field("contactPhone","联系电话",c.contactPhone,{maxLength:32})],"保存影院",v=>save(c.cinemaId ? "cinemas/"+c.cinemaId : "cinemas",v,c.cinemaId ? "PUT" : "POST"));
}
async function cinemas(view,q) {
  const result=await get(api+"cinemas",{page:q.page || 1});
  view.append(fold("新增影院",cinemaEditor()));
  for (const c of result.items) view.append(box("panel",el("h3",c.cinemaName+" · "+(isActive(c) ? "营业" : "已停用")),el("p",c.address+" · "+c.contactPhone),
    link("管理影厅",url("admin/halls",{cinemaId:c.cinemaId})),fold("编辑资料",cinemaEditor(c)),
    isActive(c) ? confirmAction("停用影院","cinemas/"+c.cinemaId+"/deactivate") : null));
  view.append(pager(result));
}
async function halls(view,q) {
  const path="cinemas/"+q.cinemaId+"/halls";
  const [c,result]=await Promise.all([get(api+"cinemas/"+q.cinemaId),get(api+path,{page:q.page || 1})]);
  view.append(link("返回影院","admin/cinemas"),el("h2",c.cinemaName),fold("新增影厅",form([field("hallName","影厅名称","",{maxLength:64})],"保存",v=>save(path,v))));
  for (const h of result.items) view.append(box("panel",el("h3",h.hallName+" · "+(isActive(h) ? "启用" : "停用")),
    link("查看与编辑座位布局",url("admin/layout",{hallId:h.hallId})),fold("修改厅名",form([field("hallName","影厅名称",h.hallName,{maxLength:64})],"保存",v=>save("halls/"+h.hallId,v,"PUT"))),
    isActive(h) ? confirmAction("停用影厅","halls/"+h.hallId+"/deactivate") : null));
  view.append(pager(result));
}
async function layout(view,q) {
  const path="halls/"+q.hallId;
  const [h,seats]=await Promise.all([get(api+path),get(api+path+"/seats")]);
  const initial=seats.map(s=>[s.rowNumber,s.seatNumber,isActive(s) ? "可售" : "禁用"].join(",")).join("\n");
  view.append(link("返回影厅",url("admin/halls",{cinemaId:h.cinemaId})),el("h2",h.hallName+" · "+seats.length+" 座"),
    table(["排","座","状态"],seats.map(s=>[s.rowNumber,s.seatNumber,isActive(s) ? "可售" : "禁用"])),
    fold("编辑整厅布局",el("p","每行输入 排号,座号,可售或禁用，例如 1,2,可售。保存会替换整个布局，已有排片的影厅布局已冻结。"),
      form([field("layout","全部座位",initial,{type:"textarea"})],"保存整厅布局",v=>{
        const entries=v.layout.trim().split(/\r?\n/).map((line,i)=>{
          const match=line.trim().match(/^(\d+)\s*[,，]\s*(\d+)\s*[,，]\s*(可售|禁用)$/);
          if (!match) throw new Error("第 "+(i+1)+" 行格式有误。");
          return {rowNumber:+match[1],seatNumber:+match[2],active:match[3]==="可售"};
        });
        if (confirm("确认替换为这 "+entries.length+" 个座位？")) return save(path+"/seats",{seats:entries});
      })));
}
function cinemaOptions(cinemas) { return cinemas.map(c=>[c.cinemaId,c.cinemaName+" #"+c.cinemaId]); }
async function screenings(view,q) {
  const dateValue=q.date || day();
  const [cinemas,movies,result]=await Promise.all([all(api+"cinemas"),all(api+"movies"),
    get(api+"screenings",{date:dateValue,cinemaId:q.cinemaId,movieId:q.movieId,page:q.page || 1})]);
  const movieOptions=movies.map(m=>[m.movieId,m.title+" #"+m.movieId]);
  view.append(filter("screenings",[date("date","开场日期",dateValue),select("cinemaId","影院",[["","全部"],...cinemaOptions(cinemas)],q.cinemaId,true),
    select("movieId","影片",[["","全部"],...movieOptions],q.movieId,true)]));
  view.append(link("新增排片",url("admin/screening-edit",{date:dateValue,cinemaId:q.cinemaId})));
  view.append(table(["场次","影片","影厅","放映时间","票价","状态","操作"],result.items.map(s=>[s.screeningId,s.movieTitle,s.cinemaName+" · "+s.hallName,period(s.startsAt,s.endsAt),"¥"+s.ticketPrice,states[s.status],
    s.status==="SCHEDULED" ? box("actions",link("编辑",url("admin/screening-edit",{id:s.screeningId,date:dateValue,cinemaId:s.cinemaId})),confirmAction("取消排片","screenings/"+s.screeningId+"/cancel",{expectedRevision:s.revision})) : "—"])),pager(result));
}
async function screeningEditor(view,q) {
  const [s,cinemas,movies]=await Promise.all([q.id ? get(api+"screenings/"+q.id) : null,all(api+"cinemas"),all(api+"movies")]);
  const active=cinemas.filter(isActive);
  const cinemaId=q.cinemaId || s?.cinemaId || active[0]?.cinemaId;
  view.append(link("返回排片",url("admin/screenings",{date:q.date})));
  if (!cinemaId) { view.append(el("p","请先建立并启用影院。")); return; }
  view.append(form([select("cinemaId","选择排片影院",cinemaOptions(active),cinemaId)],"载入影厅",v=>go("admin/screening-edit",{...q,...v}),"filters"));
  const hs=(await all(api+"cinemas/"+cinemaId+"/halls")).filter(isActive);
  const ms=movies.filter(m=>m.listingStatus==="LISTED");
  if (!hs.length || !ms.length) { view.append(el("p","排片需要已上架影片和启用的影厅。")); return; }
  const hallId=s ? hs.find(h=>h.hallId===s.hallId)?.hallId ?? "" : hs[0].hallId;
  view.append(form([
    select("movieId","影片",ms.map(m=>[m.movieId,m.title]),s?.movieId || ms[0].movieId),
    select("hallId","影厅",[["","请选择影厅"],...hs.map(h=>[h.hallId,h.hallName])],hallId),
    field("startsAt","开场时间（北京时间）",s ? time(s.startsAt).replace(" ","T")+":"+String(new Date(s.startsAt).getUTCSeconds()).padStart(2,"0") : (q.date || day())+"T19:00",{type:"datetime-local",step:1}),
    number("ticketPrice","票价（元）",s?.ticketPrice,{min:"0.01",max:"9999.99",step:"0.01"})
  ],"保存排片",async v=>{
    const payload={movieId:+v.movieId,hallId:+v.hallId,startsAt:v.startsAt+(v.startsAt.length===16 ? ":00" : "")+"+08:00",ticketPrice:Number(v.ticketPrice)};
    if (s) payload.expectedRevision=s.revision;
    await write(api+"screenings"+(s ? "/"+s.screeningId : ""),s ? "PUT" : "POST",payload);
    go("admin/screenings",{date:v.startsAt.slice(0,10),cinemaId});
  }));
}
async function orders(view,q) {
  const params={cinemaId:q.cinemaId,status:q.status,createdDateFrom:q.createdDateFrom || day(),createdDateTo:q.createdDateTo || day(),page:q.page || 1};
  const [cs,result]=await Promise.all([all(api+"cinemas"),get(api+"orders",params)]);
  view.append(filter("orders",[select("cinemaId","影院",[["","全部"],...cinemaOptions(cs)],q.cinemaId,true),select("status","状态",[["","全部"],...orderStates],q.status,true),
    date("createdDateFrom","下单起日",params.createdDateFrom),date("createdDateTo","下单止日",params.createdDateTo)]),orderTable(result,true),pager(result));
}
async function reviews(view,q) {
  const result=await get(api+"reviews",{movieId:q.movieId,visibility:q.visibility,page:q.page || 1});
  view.append(filter("reviews",[number("movieId","影片编号",q.movieId,{optional:true}),select("visibility","可见性",[["","全部"],["PUBLIC","公开"],["HIDDEN","隐藏"]],q.visibility,true)]));
  for (const r of result.items) view.append(box("panel",el("h3",r.authorName+" · "+r.rating+" 星 · "+states[r.visibilityStatus]),el("p","影片 #"+r.movieId+" · "+time(r.updatedAt)),prose(r.body),
    fold("更改可见性",form([field("reason","处理原因","",{maxLength:255})],r.visibilityStatus==="PUBLIC" ? "隐藏评论" : "恢复公开",
      v=>save("reviews/"+r.reviewId+"/visibility",{reason:v.reason,visible:r.visibilityStatus!=="PUBLIC"},"PUT")))));
  view.append(pager(result));
}
async function audit(view,q) {
  const params={actorId:q.actorId,actionType:q.actionType,from:q.from || day(),to:q.to || day(),page:q.page || 1};
  const result=await get(api+"audit-logs",params);
  view.append(filter("audit",[date("from","操作起日",params.from),date("to","操作止日",params.to),
    number("actorId","操作人编号",q.actorId,{optional:true}),field("actionType","操作类型",q.actionType,{optional:true,placeholder:"例如 REFUND"})]),
    table(["时间","操作人","操作","对象","摘要 / 原因","请求号"],result.items.map(a=>[time(a.occurredAt),a.actorUserId,a.actionType,a.targetType+" #"+a.targetId,[a.changeSummary,a.actionReason].filter(Boolean).join(" · "),a.requestId])),pager(result));
}
const pct=n=>n==null ? "—" : n+"%";
async function reports(view,q) {
  const cinemas=await all(api+"cinemas");
  if (!cinemas.length) { view.append(el("p","暂无影院")); return; }
  const params={cinemaId:q.cinemaId || cinemas[0].cinemaId,from:q.from || day(),to:q.to || day()};
  const kinds=[["finance","每日资金"],["movie-ranking","影片净收入排行"],["occupancy","场次上座率"]];
  const kind=kinds.some(([k])=>k===q.kind) ? q.kind : "finance";
  view.append(filter("reports",[select("cinemaId","影院",cinemaOptions(cinemas),params.cinemaId),select("kind","报表",kinds,kind),
    date("from","起日",params.from),date("to","止日",params.to)]));
  const data=await get(api+"reports/"+kind,{...params,...(kind==="finance" ? {} : {page:q.page || 1})});
  if (kind==="finance") view.append(el("h2","按支付日及退款日统计"),table(["日期","尝试","成功","失败","成功率","售票张数","支付额（元）","退款额（元）","净额（元）"],data.map(r=>[r.businessDate,r.attempts,r.successes,r.failures,pct(r.successRate),r.soldTickets,r.paidAmount,r.refundAmount,r.netAmount])));
  if (kind==="movie-ranking") view.append(el("h2","按期间净收入排序"),table(["影片","支付额（元）","退款额（元）","净额（元）"],data.items.map(r=>[r.movieTitle,r.paidAmount,r.refundAmount,r.netAmount])),pager(data));
  if (kind==="occupancy") view.append(el("h2","按开场日统计"),el("p",data.total.capacity ? "加权上座率 "+pct(data.total.occupancyRate)+" · 已售 "+data.total.sold+" / 可售 "+data.total.capacity : "暂无可计算场次"),
    table(["场次","已售座位","可售容量","上座率"],data.screenings.items.map(r=>[r.screeningId,r.sold,r.capacity,pct(r.occupancyRate)])),pager(data.screenings));
}
export async function admin(view,q,tab="movies") {
  const pages={movies,cinemas,halls,layout,screenings,"screening-edit":screeningEditor,orders,reviews,reports,audit};
  if (!Object.hasOwn(pages,tab)) throw new Error("后台页面不存在。");
  view.append(el("h1",sections[tab] || ({halls:"影厅管理",layout:"座位布局","screening-edit":"维护排片"})[tab]),box("tabs",Object.entries(sections).map(([k,v])=>{
    const a=link(v,"admin/"+k);
    if (k===tab) a.setAttribute("aria-current","page");
    return a;
  })));
  await pages[tab](view,q);
}
