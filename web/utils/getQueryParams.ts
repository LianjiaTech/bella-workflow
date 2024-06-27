/**
 * 获取url后的参数
 * @param param
 * @returns
 */
export const getQueryParams = (param: string): string | null => {
  const urlParams = new URLSearchParams(window.location.search)
  console.log(urlParams, 'urlParams>>>>')
  return urlParams.get(param)
}
