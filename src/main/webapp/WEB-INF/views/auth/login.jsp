<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="auth-panel">
    <h1>登录</h1>
    <p class="muted">回到 AI TEF，继续你的技术讨论和资料收藏。</p>
    <c:if test="${not empty error}">
        <div class="form-error"><c:out value="${error}"/></div>
    </c:if>
    <form class="form-stack" action="${pageContext.request.contextPath}/login" method="post">
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <input type="hidden" name="redirect" value="${redirect}">
        <label>账号或邮箱
            <input name="account" autocomplete="username" required>
        </label>
        <label>密码
            <input name="password" type="password" autocomplete="current-password" required>
        </label>
        <button class="btn btn-primary btn-wide" type="submit">登录</button>
    </form>
    <p class="muted">还没有账号？<a href="${pageContext.request.contextPath}/register">现在注册</a></p>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
