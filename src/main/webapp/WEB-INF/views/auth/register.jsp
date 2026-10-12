<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="auth-panel">
    <h1>注册</h1>
    <p class="muted">创建账号后即可发帖、回复、点赞、收藏和接收通知。</p>
    <c:if test="${not empty error}">
        <div class="form-error"><c:out value="${error}"/></div>
    </c:if>
    <form class="form-stack" action="${pageContext.request.contextPath}/register" method="post">
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <label>用户名
            <input name="username" pattern="[A-Za-z0-9_]{3,40}" required>
        </label>
        <label>昵称
            <input name="displayName" required>
        </label>
        <label>邮箱
            <input name="email" type="email" required>
        </label>
        <label>密码
            <input name="password" type="password" minlength="8" required>
        </label>
        <button class="btn btn-primary btn-wide" type="submit">注册</button>
    </form>
    <p class="muted">已有账号？<a href="${pageContext.request.contextPath}/login">去登录</a></p>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
