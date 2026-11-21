import {get,write} from "./api.js";
import {box,button,el,form,go,handle,link,login,notice,refresh,route,session} from "./ui.js";
import {movies,movie} from "./catalog.js";
import {order,orders,seats} from "./trade.js";
import {admin} from "./admin.js";

function account(view,register) {
  const specs=[
    {name:"username",title:"用户名",autoComplete:"username",minLength:3,maxLength:32,pattern:"[A-Za-z0-9_]{3,32}",placeholder:"3–32 位字母、数字或下划线"},
    {name:"password",title:"密码",type:"password",autoComplete:register ? "new-password" : "current-password",minLength:12,maxLength:64}
  ];
  view.append(el("h1",register ? "注册账户" : "登录映厅"),form(specs,register ? "注册" : "登录",async values=>{
    if (register) {
      await write("/auth/register","POST",values);
      notice("注册成功，请使用新账户登录。");
      go("login");
      return;
    }
    await write("/auth/login","POST",new URLSearchParams(values));
    await get("/auth/csrf");
    session.user=await get("/auth/me");
    const target=sessionStorage.getItem("cinema.return") || "#movies";
    sessionStorage.removeItem("cinema.return");
    notice("已登录");
    location.hash=target;
  }),link(register ? "已有账户，去登录" : "注册普通用户",register ? "login" : "register"));
}
function navigation() {
  const nav=document.querySelector("#nav");
  const links=[link("影片","movies")];
  if (session.user) {
    links.push(link("我的订单","orders"));
    if (session.user.role==="ADMIN") links.push(link("管理后台","admin/movies"));
    links.push(button(session.user.username+" · 退出",async()=>{
      await write("/auth/logout");
      session.user=null;
      sessionStorage.removeItem("cinema.return");
      go("movies");
      await get("/auth/csrf");
      notice("已退出");
    }));
  } else links.push(button("登录",login));
  nav.replaceChildren(...links);
}
async function render() {
  navigation();
  const {path,q}=route();
  const [page,id]=path.split("/");
  const main=document.querySelector("#main");
  const view=el("section");
  const loading=el("p","正在加载…");
  main.replaceChildren(loading,view);
  try {
    if (["orders","order","admin","admin-order"].includes(page) && !session.user) {
      notice("请先登录，再继续操作。"); login(); return;
    }
    if (["admin","admin-order"].includes(page) && session.user.role!=="ADMIN") throw new Error("该页面需要管理员权限。");
    if (["movie","order","admin-order","seats"].includes(page) && !/^[1-9]\d*$/.test(id || "")) throw new Error("页面编号无效。");
    switch(page) {
      case "movies": await movies(view,q); break;
      case "movie": await movie(view,q,id); break;
      case "seats": await seats(view,q,id); break;
      case "orders": await orders(view,q); break;
      case "order": case "admin-order": await order(view,q,id,page==="admin-order"); break;
      case "admin": await admin(view,q,id); break;
      case "login": case "register": account(view,page==="register"); break;
      default: throw new Error("页面不存在。");
    }
  } catch(e) {
    if (!view.isConnected) return;
    if (e.status===401) handle(e);
    else view.append(box("panel",el("p",e.message),button("重新加载",refresh),link("返回影片列表","movies")));
  } finally { loading.remove(); }
}
async function start() {
  try { session.user=await get("/auth/me"); }
  catch(e) { if (e.status!==401) notice(e.message); }
  window.addEventListener("hashchange",render);
  window.addEventListener("viewrefresh",render);
  render();
}
start();
