<?xml version="1.0" encoding="iso-8859-2" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ganttproject="http://ganttproject.sf.net/"
                version="1.0">

<xsl:output method="html" indent="yes" encoding="UTF-8"/>

<xsl:include href="gantt-utils.xsl"/>

<xsl:template match="ganttproject:tasks">

<table width="800" border="0">
 <tr>
  <td bgcolor="#dddddd"><h5><xsl:value-of select="@name"/></h5></td>
  <td bgcolor="#dddddd"><h5><xsl:value-of select="@begin"/></h5></td>
  <td bgcolor="#dddddd"><h5><xsl:value-of select="@end"/></h5></td>
  <td bgcolor="#dddddd"><h5><xsl:value-of select="@milestone"/></h5></td>
  <td bgcolor="#dddddd"><h5><xsl:value-of select="@progress"/></h5></td>
  <td bgcolor="#dddddd"><h5><xsl:value-of select="@assigned-to"/></h5></td>
  <td bgcolor="#dddddd"><h5><xsl:value-of select="@notes"/></h5></td>
 </tr>
 <xsl:for-each select="ganttproject:task">
	<tr>
		<td valign="top">
			<xsl:attribute name="style">
			<xsl:choose>
			<xsl:when test="number(@depth) &gt; 0">
			<xsl:text>padding-left: </xsl:text>
			<xsl:value-of select="number(@depth)"/>
			<xsl:text>em;</xsl:text>
			</xsl:when>
			<xsl:otherwise>
			<xsl:text>text-decoration: underline;</xsl:text>
			</xsl:otherwise>
			</xsl:choose>
			</xsl:attribute>
				<b><xsl:value-of select="name"/></b></td>
		<td valign="top"><xsl:value-of select="begin"/></td>
		<td valign="top"><xsl:value-of select="end"/></td>
		<td valign="top" align="center">
			<xsl:choose>
				<xsl:when test="milestone='true'">*</xsl:when>
				<xsl:otherwise>&#160;</xsl:otherwise>
			</xsl:choose>
		</td>
		<td valign="top"><xsl:value-of select="progress"/></td>
		<td valign="top"><xsl:value-of select="assigned-to"/></td>
		<td valign="top"><pre width="40"><xsl:value-of select="notes"/></pre>
            <xsl:if test="attachment/text()">
	          <div class="attachment">
               <a href="{attachment/text()}" class="attachment">
	               <xsl:choose>
	                <xsl:when test="attachment/@display-name">
	                 <xsl:value-of select='attachment/@display-name'/>
	                </xsl:when>
	                <xsl:otherwise>
	                 <xsl:value-of select='attachment/text()'/>
	                </xsl:otherwise>
	               </xsl:choose>
               </a>
             </div>
            </xsl:if>
		</td>
	</tr>
  </xsl:for-each>
</table>
</xsl:template>

</xsl:stylesheet>
