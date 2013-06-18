<%@ page language="java" import="gov.nysenate.openleg.util.JSPHelper" pageEncoding="UTF-8"%>
<jsp:include page="/header.jsp">
    <jsp:param name="title" value=" OpenLegislation Advanced Search"/>
</jsp:include>
<style>
#advsearchbox p {
    margin:12px;
    text-align:left;
}

#advsearchboxheader {
    background:#eee;
    font-size:14pt;
    padding:3px;
}

.searchlabel {
    float:left;
    margin:3px;
    width:200px;
    text-align:right;
}

.searchinput {
    float:left;
}

.searchinput input,.searchinput select {
    width:600px;
    font-size:12pt;
    border:1px solid #ccc;
}

.searchrow {
    clear:left;
}

#search-help {
    padding:0px 10px 15px 10px;
}

#search-help ul {
    list-style:none;
    padding:5px 0px 5px 10px;
}

#advanced-search-help {
    font-style:italic;
}

.term {
    background-color:#EEEEEE;
    padding:1px;
    margin:1px;
}
.BillsOnly{
	display:none;
}
</style>

<script>
$(document).ready(function() {
	$( "#startdate" ).datepicker({
		showOtherMonths: true,
		selectOtherMonths: true
	});
	$( "#enddate" ).datepicker({
		showOtherMonths: true,
		selectOtherMonths: true
	});

    $("[value=oid]").hide();
    $( ".BillsOnly" ).hide();
    $("#sortOrderField").hide();

	$('#type').change(function(){
		if($(this).val() == 'bill' || $(this).val() == 'resolution'  ){
			$(".BillsOnly").show('slow');
			$("[value=oid]").show();
		}else{
			$(".BillsOnly").hide('slow');
			$("[value=oid]").hide();
		}
	}).change();

   var sortOrderInput = $("[name=sortOrder]");
   $("[name=sort]").change(function() {
	    var field = $(this).val();
	    switch (field) {
		    case "": sortOrderInput.val("true"); break;
		    case "when": sortOrderInput.val("true"); break;
		    case "title": sortOrderInput.val("false"); break;
		    case "sponsor": sortOrderInput.val("false"); break;
		    case "oid": sortOrderInput.val("false"); break;
	    }
   });
});
</script>
<div id="content">

	<div class="content-bg">
	<h2 class='page-title'>Open Legislation Advanced Search</h2>
		<div id="subcontent">
            <div id="search-help">
                Use the options below to create your search.
                <br/><br/>
                <div id="advanced-search-help">
                    The following several special characters and words (case sensitive) are available for all text fields:
                    <ul>
                        <li><b>AND</b>: Require both words. <span class="term">property AND tax</span> matches documents mentioning both property and tax.
                        <li><b>OR</b>: Require at least one of the words. <span class="term">debt OR loan</span> matches documents with the words debt or loan.
                        <li><b>*</b>: Represents any number of wild card characters. e.g. <span class="term">health*</span> matches health, healthcare, healthy, etc. Note that the wild card character cannot start a word. e.g. <span class="term">*health</span> does not work.
                        <li><b>?</b>: Represents a single wild card character. e.g <span class="term">defin?tely</span> matches definitely and definately. Note that the wild card character cannot start a word. e.g. <span class="term">?efinitely</span> does not work.
                        <li><b>~</b>: Fuzzy search for the preceding word. Can be used if you are unsure of spelling; e.g. <span class="term">Schenectedy~</span>.
                        <li><b>""</b>: Require an exact match. <span class="term">"student loans"</span> finds documents with the exact phrase "student loans".
                    </ul>
                    If no special words or characters are used OpenLegislation will look for an exact match of the whole value.
                </div>
            </div>
	        <form method="get" action="<%=JSPHelper.getLink(request, "/search") %>">
                <div class="searchrow">
			        <div class="searchlabel">Legislative Content Type(s):</div><div class="searchinput">
			            <select name="type" id="type">
			                <option value="">All Types</option>
			                <option value="bill">Bills (Senate &amp; Assembly)</option>
			                <option value="resolution">Resolutions</option>
			                <option value="transcript">Floor Transcripts</option>
			                <option value="meeting">Committee Meetings</option>
			                <option value="calendar">Floor &amp; Active Calendars</option>
			                <option value="action">Bill Actions</option>
			                <option value="vote">Floor &amp; Committee Votes</option>
			            </select>
			        </div>
		        </div>
		        <div class="searchrow">
		            <div class="searchlabel">Full text search:</div>
		            <div class="searchinput">
		                <input type="text" name="full" value=""/>
		            </div>
		        </div>
		        <div class="searchrow">
		            <div class="searchlabel">Last Updated between:</div>
		            <div class="searchinput">
		                <input type="text" id="startdate" name="startdate" style="width:125px"/> and <input type="text" id="enddate"  name="enddate" style="width:125px"/>
		            </div>
		        </div>
		        <div class="searchrow">
		            <div class="searchlabel">Order By:</div>
		            <div class="searchinput">
		                <select name="sort">
		                    <option value="">Best Match</option>
		                    <option value="when">Most Recently Modified</option>
		                    <option value="title">Title [A-Z]</option>
		                    <option value="sponsor">Sponsor [A-Z]</option>
		                    <option value="oid">Bill Number</option>
		                </select>
		            </div>
		        </div>
		        <div class="searchrow" id="sortOrderField">
		            <div class="searchlabel">Sort Order:</div>
		            <div class="searchinput">
		                <select name="sortOrder">
		                    <option value="true">Descending</option>
		                    <option value="false">Ascending</option>
		                </select>
		            </div>
		        </div>
	            <br style="clear:both;"/><br/>
                <div class="BillsOnly">
                    <center><b>Bills & Resolution Options</b></center>

			        <div class="searchrow">
			            <div class="searchlabel">Legislative Session Period:</div>
			            <div class="searchinput">
			                <select name="session">
			                    <option value=""></option>
			                    <option value="2009">2009-2010</option>
			                    <option value="2011">2011-2012</option>
			                    <option value="2013">2013-2014</option>
			                </select>
			            </div>
			        </div>

                    <div class="searchrow">
			            <div class="searchlabel">and Status is:</div>
			            <div class="searchinput">
			                <select name="status">
			                   <option value="" >Any</option>
			                   <option value="Passed" >Passed</option>
			                   <option value="Vetoed" >Vetoed</option>
			                   <option value="REFERRED" >Referred</option>
			                   <option value="Reported" >Reported</option>
			                   <option value="Delivered" >Delivered</option>
			                   <option value="Home Rule Request" >Home Rule Request</option>
			                   <option value="1st Report" >1st Report</option>
			                   <option value="2nd Report" >2nd Report</option>
			                   <option value="Third Reading" >Third Reading</option>
			                   <option value="Delivered to Senate" >Delivered to Senate</option>
			                   <option value="Delivered to Assembly" >Delivered to Assembly</option>
			                   <option value="Delivered to Governor" >Delivered to Governor</option>
			                   <option value="Amended" >Amended</option>
			                   <option value="Substituted" >Substituted</option>
			                   <option value="Vote Reconsidered" >Vote Reconsidered</option>
			                   <option value="Tabled" >Tabled</option>
			                   <option value="Signed" >Signed</option>
			                   <option value="Adopted" >Adopted</option>
			                </select>
			            </div>
			        </div>

                    <div class="searchrow">
	                    <div class="searchlabel">and Sponsor Memo matches:</div>
	                    <div class="searchinput"><input type="text" name="memo" value=""/></div>
	                </div>

		            <div class="searchrow">
		                <div class="searchlabel">and "Same As" Bill:</div>
		                <div class="searchinput"><input type="text" name="sameas" value=""/></div>
		            </div>

                    <div class="searchrow">
                        <div class="searchlabel">and Sponsor is:</div>
                        <div class="searchinput"><input type="text" name="sponsor" value=""/></div>
                    </div>

	                <div class="searchrow">
	                    <div class="searchlabel">and Co-Sponsors include:</div>
	                    <div class="searchinput"><input type="text" name="cosponsors" value=""/></div>
	                </div>

	                <div class="searchrow">
                        <div class="searchlabel">and current Committee is:</div>
                        <div class="searchinput"><input type="text" name="committee" value=""/></div>
                    </div>
                </div>
                <br style="clear:both;"/><br/>

	            <center><input type="submit" value="Advanced Search"/></center>
	        </form>
	    </div>
	 </div>
</div>
<jsp:include page="/footer.jsp"/>