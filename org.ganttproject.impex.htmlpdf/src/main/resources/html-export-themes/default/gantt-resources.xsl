<?xml version="1.0" encoding="iso-8859-2" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ganttproject="http://ganttproject.sf.net/"
                version="1.0">

<xsl:output method="html" indent="yes" encoding="UTF-8"/>

<xsl:include href="gantt-utils.xsl"/>

<xsl:template match="ganttproject:resources">
<table width="600" border="0">
 <tr>
  <td bgcolor="#dddddd"><h5><xsl:value-of select="@name"/></h5></td>
  <td bgcolor="#dddddd"><h5><xsl:value-of select="@role"/></h5></td>
  <td bgcolor="#dddddd"><h5><xsl:value-of select="@mail"/></h5></td>
  <td bgcolor="#dddddd"><h5><xsl:value-of select="@phone"/></h5></td>
 </tr>
  <xsl:for-each select="ganttproject:resource">
	<tr>
		<td valign="top"><b><xsl:value-of select="name"/></b></td>
		<td valign="top"><xsl:value-of select="role"/></td>
		<td valign="top"><a><xsl:attribute name="href">mailto:<xsl:value-of select="mail"/></xsl:attribute><xsl:value-of select="mail"/></a></td>
		<td valign="top"><xsl:value-of select="phone"/></td>
	</tr>
  </xsl:for-each>
</table>

<br/><br/>
<img>
	<xsl:attribute name="src"><xsl:value-of select="/ganttproject/resources/chart/@path"/></xsl:attribute>
</img>
<br/>

</xsl:template>

</xsl:stylesheet>

