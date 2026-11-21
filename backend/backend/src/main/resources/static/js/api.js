import {url} from "./ui.js";

export async function request(path,{method="GET",body,params,headers={}}={}) {
  let response;
  try {
    response = await fetch(url("/api"+path,params),{
      method,body,headers,credentials:"same-origin",cache:"no-store",signal:AbortSignal.timeout(15000)
    });
  } catch {
    throw new Error(method === "GET" ? "连接超时或网络中断，请刷新重试。" : "未收到操作结果，请核对当前状态。交易重试请使用原请求。");
  }
  const raw = await response.text();
  let data;
  try { data = raw ? JSON.parse(raw) : null; } catch { throw new Error("服务返回无法识别的内容，请核对状态后刷新。"); }
  if (!response.ok) {
    const fields = (data?.fieldErrors || []).map(f=>f.field+" "+f.message);
    const advice = {
      SCREENING_CHANGED:"请按最新场次和票价重新确认。",
      SEAT_CONFLICT:"请刷新座位图并重新选座。",
      REQUEST_KEY_REUSED:"请先在我的订单中核对原订单。",
      RETRY_REQUIRED:"稍后用原请求重试。"
    };
    const message = [
      data?.message || "请求失败（"+response.status+"）",
      ...fields,
      advice[data?.code],
      data?.requestId && "请求号 "+data.requestId
    ].filter(Boolean).join(" · ");
    throw Object.assign(new Error(message),{status:response.status, code:data?.code});
  }
  return data;
}
export const get = (path,params) => request(path,{params});
export async function write(path,method="POST",payload) {
  // 每次写入前获取当前会话的令牌，登录、退出及多标签页切换后同样适用。
  const csrf = await get("/auth/csrf");
  const headers = {[csrf.headerName]:csrf.token};
  let body;
  if (payload instanceof URLSearchParams) body=payload;
  else if (payload !== undefined) {
    headers["Content-Type"]="application/json";
    body=JSON.stringify(payload);
  }
  return request(path,{method,headers,body});
}
export async function all(path,params={}) {
  const items=[];
  for (let page=1;;page++) {
    const result=await get(path,{...params,page,pageSize:100});
    items.push(...result.items);
    if (!result.items.length || page*result.pageSize>=result.total) return items;
  }
}
