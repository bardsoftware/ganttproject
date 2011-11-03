<?xml version="1.0" encoding="iso-8859-2" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ganttproject="http://ganttproject.sf.net/"
                version="1.0">

<xsl:output method="html" indent="yes" encoding="UTF-8"/>

<xsl:include href="gantt-utils.xsl"/>

<xsl:template match="ganttproject:resources">
<style>
td.header {
  background: #dddddd;
  margin: 0;
  padding-left: 3px;
  padding-right: 3px;
  padding-top: 2px;
  padding-bottom:1px;
  border-bottom: solid black 2px;
}
td.cell {
  margin: 0;
  margin-top: 2px;
  padding-left: 3px;
  padding-right: 3px;
  padding-top: 2px;
  padding-bottom:1px;
}
div.cell {
}
h5.header {
  margin-left: 2px;
  margin-top: 2px;
}
</style>
<table border="0" cellpadding="0" cellspacing="0">
 <tr>
  <xsl:for-each select="//view[@id='resource-table']/field">
    <td valign="top" class="header">
      <h5 class="header"><xsl:value-of select="@name"/></h5>
    </td>
  </xsl:for-each>
 </tr>
  <xsl:for-each select="ganttproject:resource">
	  <xsl:variable name="current-resource" select="." />
	  <tr>
	    <xsl:for-each select="//view[@id='resource-table']/field">
	      <xsl:variable name="field-id" select="@id" />
    		<td valign="top" class="cell">
	          <xsl:for-each select='$current-resource//*[@id=$field-id]'>
	            <xsl:value-of select="text()" />
	          </xsl:for-each>
	      </td>
	    </xsl:for-each>
	    <!-- field -->
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

