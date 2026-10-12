<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="empty-state standalone">
    <h1>系统暂时开小差了</h1>
    <p>请稍后重试，或检查数据库连接配置。</p>
    <a class="btn btn-primary" href="${pageContext.request.contextPath}/latest">回到首页</a>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
