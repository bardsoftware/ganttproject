<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:ganttproject="http://ganttproject.sf.net/"
	version="1.0">


<!-- Basic Theme for PDF export from Ganttproject
     ALexandre THOMAS
		 Septembre 2003 -->

<xsl:variable name="font" select="'sans-serif'"/>
<xsl:template match="ganttproject:report">
  <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">

    <fo:layout-master-set>
      <!-- layout information -->
      <fo:simple-page-master master-name="simple"
                    page-height="21cm"
                    page-width="29cm"
                    margin-top="1cm"
                    margin-bottom="1cm"
                    margin-left="1cm"
                    margin-right="1cm">
        <fo:region-body margin-top="1cm"/>
        <fo:region-before extent="1cm"/>
        <fo:region-after extent="1cm"/>
      </fo:simple-page-master>
    </fo:layout-master-set>
    <!-- end: defines page layout -->


	    <fo:page-sequence master-reference="simple">

        <!-- pied de page -->
        <fo:static-content flow-name="xsl-region-after">
	  <fo:block text-align="right"
	      font-size="9pt" line-height="1em + 15pt">

          <xsl:attribute name="font-family">
              <xsl:value-of select="$font"/>
          </xsl:attribute>
	      page <fo:page-number/> - GanttProject
	  </fo:block>
        </fo:static-content>

         <fo:flow flow-name="xsl-region-body">

        	<fo:block font-size="15pt"
		        line-height="24pt"
		        space-after.optimum="15pt"
		        color="black"
		        text-align="center"
		        padding-top="3pt">
                <xsl:attribute name="font-family">
                    <xsl:value-of select="$font"/>
                </xsl:attribute>

		    	<xsl:apply-templates select="ganttproject:project"/>
	         </fo:block>


			 <fo:block font-size="10pt"
		        line-height="24pt"
		        space-after.optimum="15pt"
		        color="black"
		        text-align="center"
		        padding-top="2pt"
						padding-bottom="5pt">
                <xsl:attribute name="font-family">
                    <xsl:value-of select="$font"/>
                </xsl:attribute>
		    	<xsl:apply-templates select="ganttproject:tasks"/>
	         </fo:block>


			 <fo:block font-size="10pt"
		        line-height="24pt"
		        space-after.optimum="15pt"
		        color="black"
		        text-align="center"
		        padding-top="2pt">
                 <xsl:attribute name="font-family">
                     <xsl:value-of select="$font"/>
                 </xsl:attribute>
		    	<xsl:apply-templates select="ganttproject:resources"/>
	         </fo:block>


			 <fo:block font-size="10pt"
		        line-height="24pt"
		        space-after.optimum="15pt"
		        color="black"
		        text-align="center"
		        padding-top="3pt">
                 <xsl:attribute name="font-family">
                     <xsl:value-of select="$font"/>
                 </xsl:attribute>
		    	<xsl:apply-templates select="ganttproject:ganttchart"/>
	         </fo:block>

			 <fo:block font-size="10pt"
		        line-height="24pt"
		        space-after.optimum="15pt"
		        color="black"
		        text-align="center"
		        padding-top="3pt">
                 <xsl:attribute name="font-family">
                     <xsl:value-of select="$font"/>
                 </xsl:attribute>
		    	<xsl:apply-templates select="ganttproject:resourceschart"/>
	         </fo:block>
      </fo:flow>
    </fo:page-sequence>
  </fo:root>
</xsl:template>


<!-- ========================================== Project settings ========================================== -->
<xsl:template match="ganttproject:project">

    <fo:block font-size="20pt"
              line-height="16pt"
              background-color="#145277"
              color="white"
              space-after.optimum="15pt"
              text-align="center"
              padding-top="8pt"
              padding-bottom="8pt">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$font"/>
        </xsl:attribute>
         <xsl:apply-templates select="@title"/>
    </fo:block>

	<fo:block font-size="10pt"
              line-height="1pt"
              color="#145277"
              space-after.optimum="2pt"
              text-align="left"
              padding-top="15pt">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$font"/>
        </xsl:attribute>
        <xsl:apply-templates select="@name"/>
		<xsl:text> : </xsl:text>
		<xsl:apply-templates select="@nameValue"/>
    </fo:block>

	<fo:block font-size="10pt"
              line-height="1pt"
              color="#145277"
              space-after.optimum="2pt"
              text-align="left"
              padding-top="15pt">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$font"/>
        </xsl:attribute>
        <xsl:apply-templates select="@organisation"/>
		<xsl:text> : </xsl:text>
		<xsl:apply-templates select="@organisationValue"/>
    </fo:block>

	<fo:block font-size="10pt"
              line-height="1pt"
              color="#145277"
              space-after.optimum="2pt"
              text-align="left"
              padding-top="15pt">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$font"/>
        </xsl:attribute>
        <xsl:apply-templates select="@webLink"/>
		<xsl:text> : </xsl:text>
		<xsl:apply-templates select="@webLinkValue"/>
    </fo:block>

	<fo:block font-size="10pt"
              color="#145277"
              space-after.optimum="2pt"
              text-align="left"
              padding-top="15pt">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$font"/>
        </xsl:attribute>
        <xsl:apply-templates select="@description"/>
		<xsl:text> : </xsl:text>
	    <fo:block font-size="8pt"
	              start-indent="10pt"
	              line-height="10pt"
	              white-space-collapse="false">
	      <xsl:value-of select="descriptionValue/text()" />
	    </fo:block>
    </fo:block>


	 <fo:block font-size="10pt"
						line-height="1pt" color="#145277"
						space-after.optimum="2pt" text-align="left"
						padding-top="15pt">
         <xsl:attribute name="font-family">
             <xsl:value-of select="$font"/>
         </xsl:attribute>
				<xsl:text>Date : </xsl:text>
				<xsl:apply-templates select="@currentDateTimeValue"/>
		</fo:block>


</xsl:template>


<!-- ========================================== Task List ========================================== -->
<xsl:template match="ganttproject:tasks">
	<fo:block font-size="20pt"
              line-height="16pt"
              background-color="#145277"
              color="white"
              space-after.optimum="15pt"
              text-align="center"
              padding-top="8pt"
              padding-bottom="8pt"
			  break-before="page">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$font"/>
        </xsl:attribute>
        <xsl:apply-templates select="@title"/>
    </fo:block>


	<fo:block space-after.optimum="0pt" font-size="10pt" padding-bottom="30pt">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$font"/>
        </xsl:attribute>

		<fo:table>
			<fo:table-column column-width="8cm"/>	<!--Name-->
			<fo:table-column column-width="2cm"/>	<!--Begin-->
			<fo:table-column column-width="2cm"/>	<!--End-->
			<fo:table-column column-width="2cm"/>	<!--Milestone-->
			<fo:table-column column-width="1cm"/>	<!--Percent-complete-->
			<fo:table-column column-width="3cm"/>	<!--Affecter-->
			<fo:table-column column-width="9cm"/>	<!--Notes-->
			<fo:table-body>
				<fo:table-row>
					<fo:table-cell>
						<fo:block background-color="#1c84bc" color="white" font-size="11pt" space-after.optimum="0pt" space-before.optimum="0pt" text-align="left">
							<xsl:apply-templates select="@name"/>
						</fo:block>
					</fo:table-cell>
					<fo:table-cell>
						<fo:block background-color="#1c84bc" color="white" font-size="11pt" space-after.optimum="0pt" space-before.optimum="0pt" text-align="left">
							<xsl:apply-templates select="@begin"/>
						</fo:block>
					</fo:table-cell>
					<fo:table-cell>
						<fo:block background-color="#1c84bc" color="white" font-size="11pt" space-after.optimum="0pt" space-before.optimum="0pt" text-align="left">
							<xsl:apply-templates select="@end"/>
						</fo:block>
					</fo:table-cell>
					<fo:table-cell>
						<fo:block background-color="#1c84bc" color="white" font-size="11pt" space-after.optimum="0pt" space-before.optimum="0pt">
							<xsl:apply-templates select="@milestone"/>
						</fo:block>
					</fo:table-cell>
					<fo:table-cell>
						<fo:block background-color="#1c84bc" color="white" font-size="11pt" space-after.optimum="0pt" space-before.optimum="0pt">
							<xsl:apply-templates select="@progress"/>
						</fo:block>
					</fo:table-cell>
					<fo:table-cell>
						<fo:block background-color="#1c84bc" color="white" font-size="11pt" space-after.optimum="0pt" space-before.optimum="0pt">
							<xsl:apply-templates select="@assigned-to"/>
						</fo:block>
					</fo:table-cell>
					<fo:table-cell>
						<fo:block background-color="#1c84bc" color="white" font-size="11pt" space-after.optimum="0pt" space-before.optimum="0pt">
							<xsl:apply-templates select="@notes"/>
						</fo:block>
					</fo:table-cell>
				</fo:table-row>



				<xsl:for-each select="ganttproject:task">

							<fo:table-row>
								<fo:table-cell>
									<fo:block color="black" font-size="9pt" space-after.optimum="0pt" space-before.optimum="0pt" line-height="13pt" text-align="left">
										<!-- <xsl:attribute name="background-color"><xsl:value-of select="color"/></xsl:attribute> -->
										<xsl:value-of select="name"/>
									</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block color="black" font-size="9pt" space-after.optimum="0pt" space-before.optimum="0pt" line-height="13pt" text-align="left">
										<!-- <xsl:attribute name="background-color"><xsl:value-of select="color"/></xsl:attribute> -->
										<xsl:value-of select="begin"/>
									</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block color="black" font-size="9pt" space-after.optimum="0pt" space-before.optimum="0pt" line-height="13pt" text-align="left">
										<!-- <xsl:attribute name="background-color"><xsl:value-of select="color"/></xsl:attribute> -->
										<xsl:value-of select="end"/>
									</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block color="black" font-size="9pt" space-after.optimum="0pt" space-before.optimum="0pt" line-height="13pt">
										<!-- <xsl:attribute name="background-color"><xsl:value-of select="color"/></xsl:attribute> -->
										<xsl:choose>
											<xsl:when test="milestone='true'">*</xsl:when>
											<xsl:otherwise>&#160;</xsl:otherwise>
										</xsl:choose>
										<!-- xsl:value-of select="milestone"/> -->
									</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block color="black" font-size="9pt" space-after.optimum="0pt" space-before.optimum="0pt" line-height="13pt">
										<!-- <xsl:attribute name="background-color"><xsl:value-of select="color"/></xsl:attribute> -->
										<xsl:value-of select="progress"/>
									</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block color="black" font-size="9pt" space-after.optimum="0pt" space-before.optimum="0pt" line-height="13pt">
										<!-- <xsl:attribute name="background-color"><xsl:value-of select="color"/></xsl:attribute> -->
										<xsl:choose>
											<xsl:when test="contains(assigned-to,'#160;')='true'">
												<xsl:value-of select="'**Not Assigned**'"/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:value-of select="assigned-to"/>
											</xsl:otherwise>
										</xsl:choose>
										<!-- <xsl:value-of select="assigned-to"/> -->
									</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block color="black" font-size="9pt" space-after.optimum="0pt" space-before.optimum="0pt" line-height="13pt" text-align="left">
										<!-- <xsl:attribute name="background-color"><xsl:value-of select="color"/></xsl:attribute> -->
										<xsl:value-of select="notes"/>
									</fo:block>
								</fo:table-cell>
							</fo:table-row>


				</xsl:for-each>

			</fo:table-body>
		</fo:table>

	</fo:block>

</xsl:template>

<!-- ========================================== Resources List ========================================== -->
<xsl:template match="ganttproject:resources">
	<fo:block font-size="20pt"
              line-height="16pt"
              background-color="#145277"
              color="white"
              space-after.optimum="15pt"
              text-align="center"
              padding-top="8pt"
              padding-bottom="8pt"
			  break-before="page">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$font"/>
        </xsl:attribute>
        <xsl:apply-templates select="@title"/>
    </fo:block>


	<fo:block space-after.optimum="0pt" font-size="10pt"  padding-bottom="30pt">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$font"/>
        </xsl:attribute>

		<fo:table>
			<fo:table-column column-width="10cm"/>	<!--Name-->
			<fo:table-column column-width="5cm"/>	<!--Role-->
			<fo:table-column column-width="7cm"/>	<!--Mail-->
			<fo:table-column column-width="5cm"/>	<!--Phone-->
			<fo:table-body>
				<fo:table-row>
					<fo:table-cell>
						<fo:block background-color="#1c84bc" color="white" font-size="11pt" space-after.optimum="0pt" space-before.optimum="0pt" text-align="left">
							<xsl:apply-templates select="@name"/>
						</fo:block>
					</fo:table-cell>
					<fo:table-cell>
						<fo:block background-color="#1c84bc" color="white" font-size="11pt" space-after.optimum="0pt" space-before.optimum="0pt" text-align="left">
							<xsl:apply-templates select="@role"/>
						</fo:block>
					</fo:table-cell>
					<fo:table-cell>
						<fo:block background-color="#1c84bc" color="white" font-size="11pt" space-after.optimum="0pt" space-before.optimum="0pt" text-align="left">
							<xsl:apply-templates select="@mail"/>
						</fo:block>
					</fo:table-cell>
					<fo:table-cell>
						<fo:block background-color="#1c84bc" color="white" font-size="11pt" space-after.optimum="0pt" space-before.optimum="0pt" text-align="left">
							<xsl:apply-templates select="@phone"/>
						</fo:block>
					</fo:table-cell>
				</fo:table-row>



				<xsl:for-each select="ganttproject:resource">
							<fo:table-row>
								<fo:table-cell>
									<fo:block  color="black" font-size="9pt" space-after.optimum="0pt" space-before.optimum="0pt" line-height="13pt" text-align="left">
										<xsl:text>Name: </xsl:text><xsl:value-of select="name"/>
									</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block color="black" font-size="9pt" space-after.optimum="0pt" space-before.optimum="0pt" line-height="13pt" text-align="left">
										<xsl:value-of select="role"/>
									</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block color="black" font-size="9pt" space-after.optimum="0pt" space-before.optimum="0pt" line-height="13pt" text-align="left">
										<xsl:value-of select="mail"/>
									</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block color="black" font-size="9pt" space-after.optimum="0pt" space-before.optimum="0pt" line-height="13pt" text-align="left">
										<xsl:value-of select="phone"/>
									</fo:block>
								</fo:table-cell>
							</fo:table-row>
				</xsl:for-each>

			</fo:table-body>
		</fo:table>
	</fo:block>

</xsl:template>

<!-- ========================================== GanttChart ========================================== -->
<xsl:template match="ganttproject:ganttchart">
    <fo:block font-size="20pt"
              line-height="16pt"
              background-color="#145277"
              color="white"
              space-after.optimum="15pt"
              text-align="center"
              padding-top="8pt"
              padding-bottom="8pt"
		break-before="page">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$font"/>
        </xsl:attribute>
        <xsl:apply-templates select="@title"/>
    </fo:block>

    <!-- Show the real chart from a picture -->
    <fo:block text-align="right">
		<fo:external-graphic>
			<xsl:attribute name="src"><xsl:value-of select="@src"/></xsl:attribute>
			<xsl:attribute name="height">24cm</xsl:attribute>
		</fo:external-graphic>
    </fo:block>
</xsl:template>

<!-- ========================================== ResourcesCHART ========================================== -->
<xsl:template match="ganttproject:resourceschart">
    <fo:block font-size="20pt"
              line-height="16pt"
              background-color="#145277"
              color="white"
              space-after.optimum="15pt"
              text-align="center"
              padding-top="8pt"
              padding-bottom="8pt"
			  break-before="page">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$font"/>
        </xsl:attribute>
        <xsl:apply-templates select="@title"/>
    </fo:block>

    <!-- Show the real chart from a picture -->
    <fo:block text-align="right">
		<fo:external-graphic>
			<xsl:attribute name="src"><xsl:value-of select="@src"/></xsl:attribute>
			<xsl:attribute name="height">24cm</xsl:attribute>
		</fo:external-graphic>
    </fo:block>
</xsl:template>


</xsl:stylesheet>


