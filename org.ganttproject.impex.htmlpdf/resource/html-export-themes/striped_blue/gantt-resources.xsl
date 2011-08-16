<?xml version="1.0" encoding="iso-8859-2" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ganttproject="http://ganttproject.sf.net/" version="1.0">

<xsl:output method="html" indent="yes" encoding="UTF-8"/>

<xsl:include href="gantt-utils.xsl"/>

<xsl:variable name="odd">#dbfff8</xsl:variable>
<xsl:variable name="even">#9bc9ef</xsl:variable>

<xsl:template match="ganttproject:resources">

<table width="602" border="0" cellpadding="0" cellspacing="0" bgcolor="#002000">
<tr><td colspan="3" height="1"/></tr>
<tr><td width="1" bgcolor="#002000"/>
<td>
<table width="600" border="0" cellpadding="0" cellspacing="0" bgcolor="#e3f49c">
 <tr>
  <th><xsl:value-of select="@name"/></th><td width="1" bgcolor="#002000"/>
  <th><xsl:value-of select="@role"/></th><td width="1" bgcolor="#002000"/>
  <th><xsl:value-of select="@mail"/></th><td width="1" bgcolor="#002000"/>
  <th><xsl:value-of select="@phone"/></th>
 </tr>
  <xsl:for-each select="ganttproject:resource">
	<tr><td colspan="7" height="1" bgcolor="#002000"/></tr>
	<tr>
		<xsl:choose>
        		<xsl:when test="position() mod 2 = 0">
            			<xsl:attribute name="bgcolor"><xsl:value-of select="$even"/></xsl:attribute>
        		</xsl:when>
        		<xsl:when test="position() mod 2 = 1">
            			<xsl:attribute name="bgcolor"><xsl:value-of select="$odd"/></xsl:attribute>
        		</xsl:when>
    		</xsl:choose>
		<td valign="top"><b><xsl:value-of select="name"/></b></td><td width="1" bgcolor="#002000"/>
		<td valign="top"><xsl:value-of select="role"/></td><td width="1" bgcolor="#002000"/>
		<td valign="top"><a><xsl:attribute name="href">mailto:<xsl:value-of select="mail"/></xsl:attribute><xsl:value-of select="mail"/></a></td><td width="1" bgcolor="#002000"/>
		<td valign="top"><xsl:value-of select="phone"/></td>
	</tr>
  </xsl:for-each>
</table>
</td>
<td width="1" bgcolor="#002000"/>
</tr>
<tr><td colspan="3" height="1"/></tr>
</table>
<br/><br/>
<img>
	<xsl:attribute name="src"><xsl:value-of select="/ganttproject/resources/chart/@path"/></xsl:attribute>
</img>
<br/>
<br/><br/><br/>
<b><xsl:value-of select="/ganttproject/footer/@version"/></b><br />
<b><a href="http://ganttproject.sourceforge.net">ganttproject.sf.net</a></b><br />
<b><xsl:value-of select="/ganttproject/footer/@date"/></b>
<br/>

</xsl:template>

</xsl:stylesheet>

