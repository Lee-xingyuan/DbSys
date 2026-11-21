import {get,all,write} from "./api.js";
import {el,box,button,link,form,fold,pager,table,poster,prose,states,session,day,period,time,url,go,refresh,notice} from "./ui.js";

export async function movies(view,q) {
  const result=await get("/movies",{keyword:q.keyword,page:q.page || 1,pageSize:12});
  view.append(el("h1","正在上映"),form([
    {name:"keyword",title:"片名",value:q.keyword,optional:true,maxLength:120,placeholder:"搜索影片"}
  ],"搜索",values=>go("movies",values),"filters"));
  const cards=box("grid");
  for (const m of result.items) {
    const card=link("",url("movie/"+m.movieId,{keyword:q.keyword,listPage:q.page}));
    card.className="card";
    card.append(poster(m),el("h3",m.title),el("p",m.genre+" · "+m.durationMinutes+" 分钟"));
    cards.append(card);
  }
  view.append(result.items.length ? cards : el("p","没有找到影片，试试其他片名。"),pager(result));
}
function reviewForm(movieId,review) {
  return form([
    {name:"rating",title:"评分",options:[5,4,3,2,1].map(n=>[n,n+" 星"]),value:review?.rating ?? 5},
    {name:"body",title:"评论正文",type:"textarea",maxLength:500,value:review?.body || ""}
  ],review ? "保存修改" : "发表评论", async values=>{
    const result=await write(review ? "/reviews/"+review.reviewId : "/movies/"+movieId+"/reviews",
      review ? "PUT" : "POST",{rating:Number(values.rating),body:values.body});
    notice("评论已保存 · 编号 "+result.reviewId+" · "+states[result.visibilityStatus]);
    refresh();
  });
}
export async function movie(view,q,id) {
  const date=q.date || day();
  const m=await get("/movies/"+id);
  const rating=m.averageRating == null
    ? (m.reviewCount == null ? "评分暂不可用" : "暂无评分")
    : m.averageRating+" / 5 · "+m.reviewCount+" 条公开评价";
  view.append(link("返回影片列表",url("movies",{keyword:q.keyword,page:q.listPage})),box("feature",poster(m),
    box("",el("h1",m.title),el("p",m.genre+" · "+m.durationMinutes+" 分钟"),el("p",rating),prose(m.synopsis))));

  const user=session.user;
  const [cinemas,shows,reviews,own]=await Promise.allSettled([
    all("/cinemas"),
    get("/movies/"+id+"/screenings",{date,cinemaId:q.cinemaId,page:q.page || 1,pageSize:10}),
    get("/movies/"+id+"/reviews",{page:q.reviews || 1,pageSize:10}),
    user ? get("/reviews/me",{movieId:id}) : null
  ]);
  if (!view.isConnected) return;

  const filters=[{name:"date",title:"放映日期（北京时间）",type:"date",value:date}];
  view.append(el("h2","选择场次"));
  if (cinemas.status==="fulfilled") filters.push({
    name:"cinemaId",title:"影院",optional:true,value:q.cinemaId || "",
    options:[["","全部影院"],...cinemas.value.map(c=>[c.cinemaId,c.cinemaName])]
  });
  else view.append(el("p","影院筛选暂不可用。"));
  view.append(form(filters,"查看场次",v=>go("movie/"+id,{...q,...v,page:1}),"filters"));
  if (shows.status==="fulfilled") {
    const rows=shows.value.items.map(s=>{
      const selection=url("seats/"+s.screeningId,{...q,movieId:id,date});
      return [period(s.startsAt,s.endsAt),s.cinemaName+" · "+s.hallName,s.cinemaAddress,"¥"+s.ticketPrice,states[s.salesStatus],s.salesStatus==="SELLING" ? link("选座",selection) : "—"];
    });
    view.append(rows.length ? table(["放映时间","影院 / 影厅","地址","票价","状态",""],rows) : el("p","当前条件下暂无排片。"),pager(shows.value));
  } else view.append(el("p","场次暂不可用。"));

  view.append(el("h2","观众评价"));
  if (reviews.status==="fulfilled") {
    for (const r of reviews.value.items) view.append(box("panel",el("strong",r.authorName+" · "+r.rating+" 星"),prose(r.body),el("small",time(r.updatedAt)+" · 评论 #"+r.reviewId)));
    if (!reviews.value.total) view.append(el("p","暂无公开评论"));
    view.append(pager(reviews.value,"reviews"));
  } else view.append(el("p","公开评论暂不可用。"));
  if (user) {
    if (own.status==="fulfilled") {
      const review=own.value;
      const help=review?.visibilityStatus==="HIDDEN" ? "这条评论当前已隐藏。" : "观影结束后可评价，每人每片一条。";
      view.append(fold(review ? "修改我的评论" : "发表评论",el("p",help),reviewForm(id,review)));
    } else view.append(el("p","暂时无法读取你的评论。"));
  }
  if ([cinemas,shows,reviews,own].some(result=>result.status==="rejected")) {
    view.append(button("重新加载详情",refresh));
  }
}
