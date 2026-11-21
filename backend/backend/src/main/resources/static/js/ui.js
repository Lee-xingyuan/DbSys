export const session = { user: null };
export const states = {
  PENDING:"待支付", PAID:"已支付", CANCELLED:"已取消", EXPIRED:"已超时", REFUNDED:"已退款",
  SELLING:"可售", STOPPED:"已停售", SOLD_OUT:"已售罄", TEMPORARILY_FULL:"暂时占满",
  SCHEDULED:"已排片", LISTED:"已上架", UNLISTED:"已下架", DRAFT:"草稿", PUBLIC:"公开", HIDDEN:"已隐藏"
};
export const orderStates = Object.keys(states).slice(0,5).map(key => [key,states[key]]);
export function el(tag, ...children) {
  const node = document.createElement(tag);
  for (const child of children.flat()) {
    if (child !== null && child !== undefined) node.append(child);
  }
  return node;
}
export function box(cls,...children) {
  const node = el("div",...children);
  node.className = cls;
  return node;
}
export function link(label, hash) {
  const a = el("a", label);
  a.href = "#" + hash;
  return a;
}
export function notice(message = "") {
  document.querySelector("#notice").textContent = message;
}
export function route() {
  const [path,search] = location.hash.slice(1).split("?");
  return { path:path || "movies", q:Object.fromEntries(new URLSearchParams(search)) };
}
export function url(path, params = {}) {
  const query = new URLSearchParams();
  for (const [k,v] of Object.entries(params)) if (v !== "" && v != null) query.set(k,v);
  return path + (query.size ? "?" + query : "");
}
export function go(path, params) {
  const hash = "#" + url(path,params);
  if (location.hash === hash) refresh();
  else location.hash = hash;
}
export function refresh() { window.dispatchEvent(new Event("viewrefresh")); }
export function login() {
  if (route().path !== "login") sessionStorage.setItem("cinema.return",location.hash);
  go("login");
}
export function handle(error) {
  notice(error.message);
  if (error.status === 401 && route().path !== "login") {
    session.user = null;
    login();
  }
}
export function button(label,action) {
  const node = el("button",label);
  node.type = "button";
  node.onclick = () => run(node,action);
  return node;
}
async function run(node,action) {
  const scope = node.closest("section,nav");
  if (node.disabled || scope?.inert) return;
  if (scope) scope.inert = true;
  node.disabled = true;
  const label = node.textContent;
  node.textContent = "处理中…";
  notice();
  try { await action(); } catch (e) { handle(e); }
  finally {
    if (scope) scope.inert = false;
    node.disabled = false;
    node.textContent = label;
  }
}
// spec 的 options 是 [值, 显示名] 数组；其余约束使用 HTML 原生属性。
export function form(specs,label,submit,cls = "") {
  const f = el("form");
  f.className = cls;
  for (const spec of specs) {
    const { name,title,type="text",value="",options,optional=false,...attrs } = spec;
    const field = el(options ? "select" : type === "textarea" ? "textarea" : "input");
    if (field.tagName === "INPUT") field.type = type;
    for (const [val,caption] of options || []) field.add(new Option(caption,val));
    Object.assign(field,{name, required:!optional},attrs);
    field.value = value;
    f.append(el("label",title,field));
  }
  const send = el("button",label);
  send.type = "submit";
  f.append(send);
  f.onsubmit = event => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(f));
    run(send,() => submit(data));
  };
  return f;
}
export function fold(title, ...children) { return el("details",el("summary",title),...children); }
export function pager(result,key="page") {
  const {path,q} = route();
  const {page,pageSize,total} = result;
  const nav = box("pager","共 "+total+" 条 · 第 "+page+" 页");
  if (page > 1) nav.append(link("上一页",url(path,{...q,[key]:page-1})));
  if (page*pageSize < total) nav.append(link("下一页",url(path,{...q,[key]:page+1})));
  return nav;
}
export function table(headings, rows) {
  if (!rows.length) return el("p","暂无记录");
  const t = el("table",el("thead",el("tr",headings.map(h=>el("th",h)))));
  t.append(el("tbody",rows.map(row=>el("tr",row.map(value=>el("td",value ?? "—"))))));
  return box("scroll",t);
}
const beijingDay = new Intl.DateTimeFormat("sv-SE",{timeZone:"Asia/Shanghai",year:"numeric",month:"2-digit",day:"2-digit"});
export function day(value=Date.now()) { return beijingDay.format(new Date(value)); }
export function time(value) {
  if (!value) return "—";
  return day(value)+" "+new Intl.DateTimeFormat("en-GB",{timeZone:"Asia/Shanghai",hour:"2-digit",minute:"2-digit",hourCycle:"h23"}).format(new Date(value));
}
export function period(start,end) {
  return time(start)+" 至 "+(day(start) !== day(end) ? "次日 " : "")+time(end).slice(11);
}
export function serverClock(serverTime) {
  const base = Date.parse(serverTime), tick = performance.now();
  return () => base + performance.now() - tick;
}
export function poster(movie) {
  const frame = box("poster",el("span",movie.title));
  if (!movie.posterUrl) return frame;
  try {
    const imageUrl = new URL(movie.posterUrl,location.origin);
    if (!["http:","https:"].includes(imageUrl.protocol)) return frame;
    const img = el("img");
    img.alt = movie.title+" 海报";
    img.onload = () => frame.replaceChildren(img);
    img.src = imageUrl.href;
  } catch { /* 保留片名占位图 */ }
  return frame;
}
export function prose(text) { const p=el("p",text); p.className="prose"; return p; }
