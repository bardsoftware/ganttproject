<?xml version="1.0" encoding="iso-8859-2" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ganttproject="http://ganttproject.sf.net/" version="1.0">

<xsl:output method="html" indent="yes" encoding="UTF-8"/>

<xsl:include href="gantt-utils.xsl"/>

<xsl:template match="/ganttproject/chart">

<table width="552" border="0" cellpadding="0" cellspacing="0" bgcolor="#002000">
<tr><td colspan="3" height="1"/></tr>
<tr><td width="1" bgcolor="#002000"/>
<td>
<table width="550" border="0" cellpadding="0" cellspacing="0" bgcolor="#dbfff8">
 <tr>
  <th><xsl:value-of select="/ganttproject/project/name/@title"/></th><td width="1" bgcolor="#002000"/>
  <th><xsl:value-of select="/ganttproject/project/organization/@title"/></th>
 </tr>
<tr><td colspan="3" height="1" bgcolor="#002000"/></tr>
<tr>
<td valign="top"><b><xsl:value-of select="/ganttproject/project/name"/></b></td><td width="1" bgcolor="#002000"/>
<td valign="top"><xsl:value-of select="/ganttproject/project/organization"/></td>
</tr>
<tr><td colspan="3" height="1" bgcolor="#002000"/></tr>
<tr bgcolor="#9bc9ef">
<td valign="top" colspan="3"><xsl:value-of select="/ganttproject/project/description"/></td>
</tr>
</table>
</td>
<td width="1" bgcolor="#002000"/>
</tr>
<tr><td colspan="3" height="1"/></tr>
</table>
<br/>
<img>
	<xsl:attribute name="src"><xsl:value-of select="."/></xsl:attribute>
</img>

<br/><br/><br/>
<b><xsl:value-of select="/ganttproject/footer/@version"/></b><br />
<b><a href="http://ganttproject.sourceforge.net">ganttproject.sf.net</a></b><br />
<b><xsl:value-of select="/ganttproject/footer/@date"/></b>
<br/>
</xsl:template>

<xsl:template match="project" />
</xsl:stylesheet>
