<cfset wee = getPageContext()>
<cfdump var="#wee.getServletContext().getAttribute("javax.websocket.server.ServerContainer")#">