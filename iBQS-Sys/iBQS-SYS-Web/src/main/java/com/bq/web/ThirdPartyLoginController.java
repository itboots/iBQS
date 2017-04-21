package com.bq.web;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bq.core.base.Parameter;
import com.bq.core.config.Resources;
import com.bq.core.support.login.LoginHelper;
import com.bq.core.support.login.ThirdPartyLoginHelper;
import com.bq.core.support.login.ThirdPartyUser;
import com.bq.provider.ISysProvider;
import org.apache.commons.lang3.StringUtils;
import com.bq.core.base.AbstractController;
import com.bq.model.SysUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.alibaba.fastjson.JSONObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 第三方登录控制类
 * 
 * @author Harvey.Wei
 * @version 2016年5月20日 下午3:12:56
 */
@Controller
@Api(value = "第三方登录接口", description = "第三方登录接口")
public class ThirdPartyLoginController extends AbstractController<ISysProvider> {
	public String getService() {
		return "sysUserService";
	}

	@RequestMapping("/sns")
	@ApiOperation(value = "用户登录", httpMethod = "GET")
	public void thirdLogin(HttpServletRequest request, HttpServletResponse response, @RequestParam("t") String type) {
		String url = getRedirectUrl(request, type);
		try {
			response.sendRedirect(url);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@RequestMapping("/sns_success")
	@ApiOperation(value = "登录成功", httpMethod = "GET")
	public String thirdLoginsuccess() {
		return "/sns/success";
	}

	@RequestMapping("/sns_bind")
	@ApiOperation(value = "用户绑定", httpMethod = "GET")
	public String thirdLoginbind() {
		return "/sns/bind";
	}

	@RequestMapping("/sns_fail")
	@ApiOperation(value = "登录失败", httpMethod = "GET")
	public String thirdLoginfail() {
		return "/sns/fail";
	}

	@RequestMapping("/callback/wx")
	@ApiOperation(value = "微信登录回调", httpMethod = "GET")
	public String wxCallback(HttpServletRequest request, ModelMap modelMap) {
		String host = request.getHeader("host");
		try {
			String code = request.getParameter("code");
			if (StringUtils.isNotBlank(code)) {// 如果不为空
				// 获取token和openid
				Map<String, String> map = ThirdPartyLoginHelper.getWxTokenAndOpenid(code, host);
				String openId = map.get("openId");
				if (StringUtils.isNotBlank(openId)) {// 如果openID存在
					// 获取第三方用户信息存放到session中
					ThirdPartyUser thirdUser = ThirdPartyLoginHelper.getWxUserinfo(map.get("access_token"), openId);
					thirdUser.setProvider("WX");
					thirdPartyLogin(thirdUser);
					// 跳转到登录成功界面
					modelMap.put("retUrl", Resources.THIRDPARTY.getString("third_login_success"));
				} else {// 如果未获取到OpenID
					modelMap.put("retUrl", "-1");
				}
			} else {// 如果没有返回令牌，则直接返回到登录页面
				modelMap.put("retUrl", "-1");
			}
		} catch (Exception e) {
			modelMap.put("retUrl", "-1");
			e.printStackTrace();
		}

		return "/sns/redirect";
	}

	@RequestMapping("/callback/qq")
	@ApiOperation(value = "QQ登录回调", httpMethod = "GET")
	public String qqCallback(HttpServletRequest request, ModelMap modelMap) {
		String host = request.getHeader("host");
		try {
			String code = request.getParameter("code");
			if (StringUtils.isNotBlank(code)) {// 如果不为空
				// 获取token和openid
				Map<String, String> map = ThirdPartyLoginHelper.getQQTokenAndOpenid(code, host);
				String openId = map.get("openId");
				if (StringUtils.isNotBlank(openId)) {// 如果openID存在
					// 获取第三方用户信息存放到session中
					ThirdPartyUser thirdUser = ThirdPartyLoginHelper.getQQUserinfo(map.get("access_token"), openId);
					thirdUser.setProvider("QQ");
					thirdPartyLogin(thirdUser);
					// 跳转到登录成功界面
					modelMap.put("retUrl", Resources.THIRDPARTY.getString("third_login_success"));
				} else {// 如果未获取到OpenID
					modelMap.put("retUrl", "-1");
				}
			} else {// 如果没有返回令牌，则直接返回到登录页面
				modelMap.put("retUrl", "-1");
			}
		} catch (Exception e) {
			modelMap.put("retUrl", "-1");
			e.printStackTrace();
		}

		return "/sns/redirect";
	}

	@RequestMapping("callback/sina")
	@ApiOperation(value = "微博登录回调", httpMethod = "GET")
	public String sinaCallback(HttpServletRequest request, ModelMap modelMap) {
		String host = request.getHeader("host");
		try {
			String code = request.getParameter("code");
			if (StringUtils.isNotBlank(code)) {// 如果不为空
				// 获取token和uid
				JSONObject json = ThirdPartyLoginHelper.getSinaTokenAndUid(code, host);
				String uid = json.getString("uid");
				if (StringUtils.isNotBlank(uid)) {// 如果uid存在
					// 获取第三方用户信息存放到session中
					ThirdPartyUser thirdUser = ThirdPartyLoginHelper.getSinaUserinfo(json.getString("access_token"),
							uid);
					thirdUser.setProvider("SINA");
					thirdPartyLogin(thirdUser);
					// 跳转到登录成功界面
					modelMap.put("retUrl", Resources.THIRDPARTY.getString("third_login_success"));
				} else {// 如果未获取到OpenID
						// 跳转到登录成功界面
					modelMap.put("retUrl", "-1");
				}
			} else {// 如果没有返回令牌，则直接返回到登录页面
					// 跳转到登录成功界面
				modelMap.put("retUrl", "-1");
			}
		} catch (Exception e) {
			// 跳转到登录失败界面
			modelMap.put("retUrl", "-1");
			e.printStackTrace();
		}

		return "/sns/redirect";
	}

	private void thirdPartyLogin(ThirdPartyUser param) {
		SysUser sysUser = null;
		// 查询是否已经绑定过
		Parameter parameter = new Parameter(getService(), "queryUserIdByThirdParty").setModel(param);
		String userId = provider.execute(parameter).getId();

		if (userId == null) {
			parameter = new Parameter(getService(), "insertThirdPartyUser").setModel(param);
			sysUser = (SysUser) provider.execute(parameter).getModel();
		} else {
			parameter = new Parameter(getService(), "queryById").setId(param.getId());
			sysUser = (SysUser) provider.execute(parameter).getModel();
		}
		LoginHelper.login(sysUser.getAccount(), sysUser.getPassword());
	}

	private String getRedirectUrl(HttpServletRequest request, String type) {
		String url = "";
		String host = request.getHeader("host");
		url = Resources.THIRDPARTY.getString("authorizeURL_" + type);
		if ("wx".equals(type)) {
			url = url + "?appid=" + Resources.THIRDPARTY.getString("app_id_" + type) + "&redirect_uri=http://" + host
					+ Resources.THIRDPARTY.getString("redirect_url_" + type) + "&response_type=code&scope="
					+ Resources.THIRDPARTY.getString("scope_" + type) + "&state=fhmj";
		} else {
			url = url + "?client_id=" + Resources.THIRDPARTY.getString("app_id_" + type) + "&response_type=code&scope="
					+ Resources.THIRDPARTY.getString("scope_" + type) + "&redirect_uri=http://" + host
					+ Resources.THIRDPARTY.getString("redirect_url_" + type);
		}
		return url;
	}
}
