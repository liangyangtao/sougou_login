package com.qizaodian.login;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.qizaodian.fetcher.HttpClientBuilder;

/***
 * 
 * @ClassName: SogouLoginByHttpClient
 * @Description: 通过httpClient 登录搜狗
 * @author: Administrator
 * @date: 2016-11-22 下午3:56:53
 */
public class SogouLoginByHttpClient {

	public static RequestConfig requestConfig = RequestConfig.custom()
			.setSocketTimeout(30000).setConnectTimeout(30000).build();
	public static BasicCookieStore cookieStore = new BasicCookieStore();

	public static CloseableHttpClient httpClient;
	public static String firstUrl = "https://account.sogou.com/web/webLogin";
	public static String loginUrl = "https://account.sogou.com/web/login";
	public static String cookieUrl = "https://pb.sogou.com/pv.gif?uigs_productid=ufo&ufoid=passport&rdk=1479803139020&img=pv.gif&b=ff&v=49&o=win6.1&s=1920x1080&l=zh-CN&bi=64&ls=1_1&refer=&page=搜狗通行证&pageUrl=https://account.sogou.com/web/webLogin&productid=passport&ptype=web&pcode=index";
	public static String callUrl = "https://account.sogou.com/static/api/jump.htm?status=0&needcaptcha=0&msg=";
	public static String ssologinUrl = "https://account.sogou.com/";

	public static void main(String[] args) {
		PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
		HttpClientBuilder httpClientBuilder = new HttpClientBuilder(false,
				poolingHttpClientConnectionManager, cookieStore);
		httpClient = httpClientBuilder.getHttpClient();
		String username = "";
		String password = "";
		SogouLoginByHttpClient sogouLoginByHttpClient = new SogouLoginByHttpClient();
		sogouLoginByHttpClient.login(username, password);

	}

	/**
	 * 登陆
	 * 
	 * */

	public boolean login(String username, String password) {
		/**
		 * 第一步：访问https://account.sogou.com/web/webLogin
		 * 
		 * 
		 * 
		 * 
		 * 
		 * */
		getHtml(httpClient, firstUrl, "");
		/***
		 * 获取Cookie
		 */
		getHtml(httpClient, cookieUrl, "");
		/***
		 * 第二步检查 用户是否可用
		 * 
		 * https://account.sogou.com/web/login/checkNeedCaptcha?username=
		 * XXXXXXXXXXXXXX&client_id=1120&t=1479801801183
		 */
		long dateTime = new Date().getTime();
		String checkUrl = "https://account.sogou.com/web/login/checkNeedCaptcha?username="
				+ username + "&client_id=1120&t=" + dateTime;

		getHtml(httpClient, checkUrl, getCookiesString());

		/***
		 * 
		 * 第三步 post https://account.sogou.com/web/login
		 * 
		 * username:"1378022176@qq.com"
		 * 
		 * password:"123456"
		 * 
		 * captcha:""
		 * 
		 * autoLogin:"1"
		 * 
		 * client_id:"1120"
		 * 
		 * xd:"https://account.sogou.com/static/api/jump.htm"
		 * 
		 * 
		 * token:"760e68de74c12d8d056236f4f21e8233"
		 */
		String token = new Token4Sogou().getToken();
		Map<String, String> params = new HashMap<String, String>();
		params.put("username", username);
		params.put("password", password);
		params.put("captcha", "");
		params.put("autoLogin", "0");

		params.put("client_id", "1120");
		params.put("xd", "https://account.sogou.com/static/api/jump.htm");

		params.put("token", token);
		post(httpClient, "", params, "utf-8", getCookiesString());

		/***
		 * 
		 * 校验一下是否登录成功
		 * 
		 * https://account.sogou.com/
		 */
		getHtml(httpClient, callUrl, getCookiesString());

		String res = getHtml(httpClient, ssologinUrl, getCookiesString());

		/**
		 * res 如果还是登录界面的代码，说明没有登录成功
		 * 
		 * 
		 * 下面就不用测了
		 * 
		 * 登陆成功后，就可以采集搜狗微信的100页资讯了
		 * 
		 * 
		 * 
		 * */
		String sougouWeixinhtml = getHtml(
				httpClient,
				"http://weixin.sogou.com/weixin?query=%E9%93%B6%E8%A1%8C&_sug_type_=&sut=769&lkt=1%2C1479803756148%2C1479803756148&_sug_=y&type=2&sst0=1479803756249&page=11&ie=utf8&w=01019900&dr=1",
				getCookiesString());

		Document document = Jsoup.parse(sougouWeixinhtml);
		Elements resultsElements = document.select("div.results");
		Elements aElements = resultsElements.first().select("h4 > a");
		for (Element element : aElements) {
			System.out.println(element.text());
		}
		Elements pageBarElements = document.select("#pagebar_container");
		for (Element element : pageBarElements) {
			System.out.println(element);
		}
		return false;

	}

	public String post(CloseableHttpClient httpClient, String url,
			Map<String, String> params, String charset, String cookie) {
		HttpClientContext context = HttpClientContext.create();
		context.setCookieStore(cookieStore);
		String useCharset = charset;
		if (charset == null) {
			useCharset = "utf-8";
		}
		String result = null;
		try {
			HttpPost httpPost = new HttpPost(url);
			fillHeader(url, httpPost, cookie);
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			if (params != null) {
				for (String key : params.keySet()) {
					nvps.add(new BasicNameValuePair(key, params.get(key)));
				}
				httpPost.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
			}
			httpPost.setConfig(requestConfig);
			CloseableHttpResponse response = httpClient.execute(httpPost,
					context);
			try {
				HttpEntity entity = response.getEntity();
				result = EntityUtils.toString(entity, useCharset);
			} finally {
				response.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public String getHtml(CloseableHttpClient httpClient, String url,
			String cookie) {
		HttpClientContext context = HttpClientContext.create();
		HttpGet httpGet = new HttpGet(url);
		fillHeaderWithCookie(url, httpGet, cookie);
		httpGet.setConfig(requestConfig);
		String chartset = null;
		String result = null;
		try {
			CloseableHttpResponse response = httpClient.execute(httpGet,
					context);
			try {
				Header heads[] = response.getAllHeaders();
				for (Header header : heads) {
					// System.out.println(header);
					if (header.getValue().toLowerCase().contains("charset")) {
						Pattern pattern = Pattern
								.compile("charset=[^\\w]?([-\\w]+)");
						Matcher matcher = pattern.matcher(header.getValue());
						if (matcher.find()) {
							chartset = matcher.group(1);
						}
					}
				}
				if (chartset == null) {
					chartset = "utf-8";
				} else {
					if (chartset.equals("gbk2312")) {
						chartset = "gbk";
					}
				}
				InputStream inputStream = response.getEntity().getContent();
				result = inputStream2String(inputStream, chartset);
			} finally {
				response.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public String getCookiesString() {
		List<Cookie> cookies = cookieStore.getCookies();
		StringBuffer sb = new StringBuffer();
		if (cookies != null) {
			for (Cookie c : cookies) {
				sb.append(c.getName() + "=" + c.getValue() + ";");
			}
		}
		System.out.println(sb.toString());
		return sb.toString();
	}

	public void fillHeaderWithCookie(String url, HttpGet httpGet, String cookie) {
		httpGet.setHeader("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:49.0) Gecko/20100101 Firefox/49.0");
		httpGet.setHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		httpGet.setHeader("Accept-Language",
				"zh-CN,zh;q=0.8,en-us;q=0.8,en;q=0.6");
		httpGet.setHeader("Accept-Encoding", "gzip, deflate,sdch");
		httpGet.setHeader("Connection", "keep-alive");
		httpGet.setHeader("Cache-Control", "no-cache");
		httpGet.setHeader("Cookie", cookie);
		httpGet.setHeader("Upgrade-Insecure-Requests:", "1");

	}

	private void fillHeader(String url, HttpPost httpPost, String cookie) {
		httpPost.setHeader("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:49.0) Gecko/20100101 Firefox/49.0");
		httpPost.setHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		httpPost.setHeader("Accept-Language",
				"zh-CN,zh;q=0.8,en-us;q=0.8,en;q=0.6");
		httpPost.setHeader("Accept-Encoding", "gzip, deflate,sdch");
		httpPost.setHeader("Connection", "keep-alive");
		httpPost.setHeader("Cache-Control", "no-cache");
		httpPost.setHeader("Cookie", cookie);
		httpPost.setHeader("Upgrade-Insecure-Requests:", "1");

	}

	public String inputStream2String(InputStream is, String charset) {
		String temp = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			int i = -1;
			while ((i = is.read()) != -1) {
				baos.write(i);
			}
			temp = baos.toString(charset);
			if (temp.contains("???") || temp.contains("�")) {
				Pattern pattern = Pattern
						.compile("<meta[\\s\\S]*?charset=\"{0,1}(\\S+?)\"\\s{0,10}/{0,1}>");
				// .compile("<meta\\s+http-equiv=\"content-type\"\\s+content=\"[\\s\\S]*?charset=(\\S+?)\"\\s+/>");
				Matcher matcher = pattern.matcher(temp.toLowerCase());
				if (matcher.find()) {
					charset = matcher.group(1);
				} else {
					charset = "gbk";
				}
				temp = baos.toString(charset);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				baos.close();
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return temp;

	}

}
