<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="empty-state standalone">
    <h1>找不到页面</h1>
    <p>你访问的内容不存在，或者已经被移动。</p>
    <a class="btn btn-primary" href="${pageContext.request.contextPath}/latest">回到首页</a>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
