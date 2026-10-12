<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>

<section class="page-title-row">
    <div>
        <h1>徽章管理</h1>
        <p class="muted">制作徽章、设置自动获得条件，也可按用户名授予或撤回徽章。</p>
    </div>
    <div class="admin-actions-inline">
        <form class="inline-form" action="${pageContext.request.contextPath}/admin/content/refresh-badges" method="post">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="returnTo" value="${pageContext.request.contextPath}/admin/badges">
            <button class="btn btn-ghost" type="submit"><i data-lucide="refresh-cw"></i>刷新自动徽章</button>
        </form>
        <a class="btn btn-ghost" href="${pageContext.request.contextPath}/badges"><i data-lucide="external-link"></i>前台徽章</a>
        <a class="btn btn-ghost" href="${pageContext.request.contextPath}/admin/dashboard"><i data-lucide="arrow-left"></i>返回后台</a>
    </div>
</section>

<c:if test="${not empty param.error}">
    <div class="form-error"><c:out value="${param.error}"/></div>
</c:if>
<c:if test="${not empty param.message}">
    <div class="form-success"><c:out value="${param.message}"/></div>
</c:if>

<section class="admin-panel" id="badge-form">
    <div class="admin-panel-head">
        <div>
            <h2>新建徽章</h2>
            <p class="muted">创建后会出现在徽章目录中，可在下方授予指定用户。</p>
        </div>
    </div>

    <form class="admin-badge-form" action="${pageContext.request.contextPath}/admin/badges/create" method="post">
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <label>
            <span>代码</span>
            <input name="code" maxlength="40" placeholder="CUSTOM_BADGE" required>
        </label>
        <label>
            <span>名称</span>
            <input name="name" maxlength="60" placeholder="特别贡献" required>
        </label>
        <label class="wide">
            <span>说明</span>
            <input name="description" maxlength="255" placeholder="为社区做出特别贡献。">
        </label>
        <label>
            <span>图标</span>
            <input name="icon" maxlength="40" value="award">
        </label>
        <label>
            <span>颜色</span>
            <input type="color" name="color" value="#0ea5e9">
        </label>
        <label>
            <span>排序</span>
            <input type="number" name="sortOrder" min="0" max="9999" value="100">
        </label>
        <label>
            <span>自动条件</span>
            <select name="ruleType">
                <option value="MANUAL">手动授予</option>
                <option value="REGISTRATION">完成注册</option>
                <option value="TOPICS">话题数达到</option>
                <option value="REPLIES">回复数达到</option>
                <option value="POSTS">发言数达到</option>
                <option value="LIKES_RECEIVED">获赞数达到</option>
                <option value="TRUST_LEVEL">信任等级达到</option>
                <option value="UPLOADS">上传数达到</option>
            </select>
        </label>
        <label>
            <span>阈值</span>
            <input type="number" name="ruleThreshold" min="0" max="9999" value="0">
        </label>
        <button class="btn btn-primary" type="submit"><i data-lucide="plus"></i>创建徽章</button>
    </form>
</section>

<section class="admin-panel" id="badge-list">
    <div class="admin-panel-head">
        <div>
            <h2>徽章目录</h2>
            <p class="muted">共 ${fn:length(badges)} 个徽章。</p>
        </div>
    </div>

    <div class="admin-badge-list">
        <c:forEach var="badge" items="${badges}">
            <article class="admin-badge-card" style="--badge-color:${badge.color}">
                <div class="admin-badge-preview">
                    <i data-lucide="${badge.icon}"></i>
                    <div>
                        <strong><c:out value="${badge.name}"/></strong>
                        <span><c:out value="${badge.code}"/> · 已授予 ${badge.awardedCount} 人</span>
                        <span class="admin-badge-rule ${badge.automaticRule ? 'auto' : ''}"><c:out value="${badge.ruleLabel}"/></span>
                    </div>
                </div>

                <form class="admin-badge-edit" action="${pageContext.request.contextPath}/admin/badges/update" method="post">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="badgeId" value="${badge.id}">
                    <label>
                        <span>代码</span>
                        <input name="code" maxlength="40" value="${fn:escapeXml(badge.code)}" required>
                    </label>
                    <label>
                        <span>名称</span>
                        <input name="name" maxlength="60" value="${fn:escapeXml(badge.name)}" required>
                    </label>
                    <label class="wide">
                        <span>说明</span>
                        <input name="description" maxlength="255" value="${fn:escapeXml(badge.description)}">
                    </label>
                    <label>
                        <span>图标</span>
                        <input name="icon" maxlength="40" value="${fn:escapeXml(badge.icon)}">
                    </label>
                    <label>
                        <span>颜色</span>
                        <input type="color" name="color" value="${badge.color}">
                    </label>
                    <label>
                        <span>排序</span>
                        <input type="number" name="sortOrder" min="0" max="9999" value="${badge.sortOrder}">
                    </label>
                    <label>
                        <span>自动条件</span>
                        <select name="ruleType">
                            <option value="MANUAL" ${badge.ruleType == 'MANUAL' ? 'selected' : ''}>手动授予</option>
                            <option value="REGISTRATION" ${badge.ruleType == 'REGISTRATION' ? 'selected' : ''}>完成注册</option>
                            <option value="TOPICS" ${badge.ruleType == 'TOPICS' ? 'selected' : ''}>话题数达到</option>
                            <option value="REPLIES" ${badge.ruleType == 'REPLIES' ? 'selected' : ''}>回复数达到</option>
                            <option value="POSTS" ${badge.ruleType == 'POSTS' ? 'selected' : ''}>发言数达到</option>
                            <option value="LIKES_RECEIVED" ${badge.ruleType == 'LIKES_RECEIVED' ? 'selected' : ''}>获赞数达到</option>
                            <option value="TRUST_LEVEL" ${badge.ruleType == 'TRUST_LEVEL' ? 'selected' : ''}>信任等级达到</option>
                            <option value="UPLOADS" ${badge.ruleType == 'UPLOADS' ? 'selected' : ''}>上传数达到</option>
                        </select>
                    </label>
                    <label>
                        <span>阈值</span>
                        <input type="number" name="ruleThreshold" min="0" max="9999" value="${badge.ruleThreshold}">
                    </label>
                    <button class="btn btn-primary" type="submit"><i data-lucide="save"></i>保存</button>
                </form>

                <div class="admin-badge-grant">
                    <form action="${pageContext.request.contextPath}/admin/badges/award" method="post">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="badgeId" value="${badge.id}">
                        <input name="username" placeholder="用户名" required>
                        <button class="btn btn-ghost" type="submit"><i data-lucide="plus-circle"></i>授予</button>
                    </form>
                    <form action="${pageContext.request.contextPath}/admin/badges/revoke" method="post">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="badgeId" value="${badge.id}">
                        <input name="username" placeholder="用户名" required>
                        <button class="btn btn-ghost" type="submit"><i data-lucide="minus-circle"></i>撤回</button>
                    </form>
                    <form action="${pageContext.request.contextPath}/admin/badges/delete" method="post" onsubmit="return confirm('确认删除该徽章？已获得记录也会一起删除。')">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="badgeId" value="${badge.id}">
                        <button class="btn btn-danger-soft" type="submit"><i data-lucide="trash-2"></i>删除</button>
                    </form>
                </div>
            </article>
        </c:forEach>
        <c:if test="${empty badges}">
            <div class="empty-state">暂无徽章。</div>
        </c:if>
    </div>
</section>

<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
