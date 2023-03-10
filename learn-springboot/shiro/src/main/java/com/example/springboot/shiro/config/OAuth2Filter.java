package com.example.springboot.shiro.config;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class OAuth2Filter extends BasicHttpAuthenticationFilter {

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
        // <1> 获取请求中的 token
        String token = getRequestToken((HttpServletRequest) request);
        // 如果不存在，则返回 null
        if (StringUtils.isBlank(token)) {
            return null;
        }

        // <2> 创建 OAuth2Token 对象
        return new OAuth2Token(token);
    }

    private String getRequestToken(HttpServletRequest httpRequest) {
        // 优先，从 header 中获取 token
        String token = httpRequest.getHeader("token");

        // 次之，如果 header 中不存在 token ，则从参数中获取 token
        if (StringUtils.isBlank(token)) {
            token = httpRequest.getParameter("token");
        }
        return token;
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        // 只允许Option方法可以直接访问，否则进入onAccessDenied()方法进行认证
        return ((HttpServletRequest) request).getMethod().equals(RequestMethod.OPTIONS.name());
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        // <1> 获取请求中的 token 。如果 token 不存在，直接返回 401 ，认证不通过
        String token = getRequestToken((HttpServletRequest) request);
        if (StringUtils.isBlank(token)) {
            // 设置响应 Header
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");

            // 返回认证不通过
            String json = "invalid token";
            httpResponse.getWriter().print(json);

            // 返回 false
            return false;
        }

        // <2> 执行登录逻辑，实际执行的是基于 Token 进行认证。
        // 调用父类 AuthenticatingFilter 的 #executeLogin(request, response) 方法，执行登录逻辑。
        // 实际上在方法内部，调用 OAuth2Realm 的 #doGetAuthenticationInfo(AuthenticationToken token) 方法，
        // 执行基于 Token 进行认证
        return executeLogin(request, response);
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
        // 设置响应 Header
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setContentType("application/json;charset=utf-8");
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        try {
            // 处理登录失败的异常
            Throwable throwable = e.getCause() == null ? e : e.getCause();

            // 返回认证不通过
            String json = "认证失败";
            httpResponse.getWriter().print(json);
        } catch (IOException ignored) {
        }

        // 返回 false
        return false;
    }
}
