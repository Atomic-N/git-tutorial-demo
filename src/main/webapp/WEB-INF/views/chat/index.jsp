<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="page-title-row">
    <div>
        <h1>聊天频道</h1>
        <p class="muted">参考 Linux.do 的轻量聊天室，适合实时交流和快速协作。</p>
    </div>
</section>

<c:if test="${not empty param.error}">
    <div class="form-error"><c:out value="${param.error}"/></div>
</c:if>

<section class="chat-layout">
    <aside class="chat-sidebar">
        <c:forEach var="item" items="${channels}">
            <a class="chat-channel ${channel != null && channel.id == item.id ? 'active' : ''}" href="${pageContext.request.contextPath}/chat/${item.slug}">
                <div>
                    <strong><i data-lucide="${item.icon}"></i><c:out value="${item.name}"/></strong>
                    <p><c:out value="${item.description}"/></p>
                </div>
                <span>${item.messageCount}</span>
            </a>
        </c:forEach>
        <c:if test="${empty channels}">
            <div class="empty-state">当前没有可访问的聊天频道。</div>
        </c:if>
    </aside>

    <div class="chat-panel">
        <c:choose>
            <c:when test="${not empty channel}">
                <div class="chat-panel-head">
                    <div>
                        <h2><c:out value="${channel.name}"/></h2>
                        <p class="muted"><c:out value="${channel.description}"/> · 需要 Lv${channel.minTrustLevel}</p>
                    </div>
                    <span class="mini-badge">${channel.messageCount} 条消息</span>
                </div>
                <div class="chat-stream">
                    <c:forEach var="message" items="${messages}">
                        <article class="chat-message" id="message-${message.id}">
                            <img class="avatar" src="${message.authorAvatarUrl}" alt="">
                            <div>
                                <div class="chat-meta"><strong><c:out value="${message.authorDisplayName}"/></strong><span>@<c:out value="${message.authorUsername}"/></span><span>${message.createdAgo}</span></div>
                                <c:if test="${not empty message.replyPreview}"><div class="chat-reply-ref"><c:out value="${message.replyPreview}"/></div></c:if>
                                <div class="cooked-content compact">${message.cookedContent}</div>
                            </div>
                        </article>
                    </c:forEach>
                    <c:if test="${empty messages}">
                        <div class="empty-state">还没有消息，发一条开始交流。</div>
                    </c:if>
                </div>
                <c:choose>
                    <c:when test="${not empty currentUser}">
                        <form class="chat-form" action="${pageContext.request.contextPath}/chat/${channel.slug}" method="post">
                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                            <input type="hidden" name="channel" value="${channel.slug}">
                            <textarea name="content" rows="4" placeholder="写点实时讨论、排障线索或协作进度" required></textarea>
                            <div class="form-actions">
                                <button class="btn btn-primary" type="submit"><i data-lucide="send"></i>发送</button>
                            </div>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <div class="empty-state">登录后即可加入频道聊天。</div>
                    </c:otherwise>
                </c:choose>
            </c:when>
            <c:otherwise>
                <div class="empty-state standalone">当前没有可显示的频道。</div>
            </c:otherwise>
        </c:choose>
    </div>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
