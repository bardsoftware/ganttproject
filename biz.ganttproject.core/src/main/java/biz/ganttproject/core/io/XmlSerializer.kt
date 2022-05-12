/*
Copyright 2020 BarD Software s.r.o, GanttProject Cloud OU, Dmitry Kazakov

This file is part of GanttProject, an open-source project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package biz.ganttproject.core.io

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.*
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import java.math.BigDecimal

@JacksonXmlRootElement(localName = "project")
@JsonPropertyOrder(
  "name", "company", "webLink", "view-date", "view-index", "gantt-divider-location", "resource-divider-location", "version", "locale",
  "description", "views", "calendars", "tasks", "resources", "allocations", "vacations", "previous", "roles"
)
data class XmlProject (
  @get:JacksonXmlProperty(isAttribute = true, localName = "name") var name: String = "",
  @get:JacksonXmlProperty(isAttribute = true, localName = "company") var company: String = "",
  @get:JacksonXmlProperty(isAttribute = true, localName = "webLink") var webLink: String = "",
  @get:JacksonXmlProperty(isAttribute = true, localName = "view-date") var viewDate: String = "",
  @get:JacksonXmlProperty(isAttribute = true, localName = "view-index") var viewIndex: Int = 0,
  @get:JacksonXmlProperty(isAttribute = true, localName = "gantt-divider-location") var ganttDividerLocation: Int = 300,
  @get:JacksonXmlProperty(isAttribute = true, localName = "resource-divider-location") var resourceDividerLocation: Int = 300,
  @get:JacksonXmlProperty(isAttribute = true) var version: String = "",
  @get:JacksonXmlProperty(isAttribute = true) var locale: String = "",

  @get:JacksonXmlCData
  var description: String? = null,

  @get:JacksonXmlElementWrapper(useWrapping = false)
  @get:JacksonXmlProperty(localName = "view")
  var views: List<XmlView> = listOf(XmlView("gantt-chart"), XmlView("resource-table")),

  @get:JacksonXmlProperty(localName = "calendars")
  var calendars: XmlCalendars = XmlCalendars(),

  var tasks: XmlTasks = XmlTasks(),
  @get:JsonSetter(nulls = Nulls.AS_EMPTY)

  var resources: XmlResources = XmlResources(),

  @get:JacksonXmlElementWrapper(localName = "allocations")
  @get:JacksonXmlProperty(localName = "allocation")
  var allocations: List<XmlAllocation> = emptyList(),

  @get:JacksonXmlElementWrapper(localName = "vacations")
  @get:JacksonXmlProperty(localName = "vacation")
  var vacations: List<XmlVacation> = emptyList(),

  @get:JacksonXmlProperty(localName = "previous")
  var baselines: XmlBaselineList? = null,

  @get:JacksonXmlElementWrapper(useWrapping = false)
  var roles: List<XmlRoles> = emptyList()
)

@JsonPropertyOrder("zooming-state", "id")
data class XmlView(
  @get:JacksonXmlProperty(isAttribute = true) var id: String = "",
  @get:JacksonXmlProperty(isAttribute = true, localName = "zooming-state")
  var zoomingState: String? = null,
  @get:JacksonXmlElementWrapper(useWrapping = false)
  @get:JacksonXmlProperty(localName = "field")
  @get:JsonInclude(JsonInclude.Include.NON_NULL)
  var fields: List<XmlField>? = null,
  @get:JsonInclude(JsonInclude.Include.NON_NULL)
  @get:JacksonXmlCData
  var timeline: String = "",
  @get:JacksonXmlElementWrapper(useWrapping = false)
  @get:JacksonXmlProperty(localName = "option")
  @get:JsonInclude(JsonInclude.Include.NON_NULL)
  var options: List<XmlOption>? = null
) {
  data class XmlField(
    @get:JacksonXmlProperty(isAttribute = true) var id: String = "",
    @get:JacksonXmlProperty(isAttribute = true) var name: String = "",
    @get:JacksonXmlProperty(isAttribute = true) var width: Int = -1,
    @get:JacksonXmlProperty(isAttribute = true) var order: Int = -1
  )

  data class XmlOption(
    @get:JacksonXmlProperty(isAttribute = true) var id: String = "",
    @get:JacksonXmlProperty(isAttribute = true) var value: String = "",
    @get:JacksonXmlText @get:JacksonXmlCData var text: String? = null
  )
}

data class XmlCalendars(
  @get:JacksonXmlProperty(localName = "base-id", isAttribute = true)
  @get:JsonInclude(JsonInclude.Include.NON_NULL)
  var baseId: String? = null,

  @get:JacksonXmlProperty(localName = "day-types")
  var dayTypes: XmlDayTypes = XmlDayTypes(),

  @get:JacksonXmlElementWrapper(useWrapping = false)
  @get:JacksonXmlProperty(localName = "date")
  @get:JsonInclude(JsonInclude.Include.NON_NULL)
  var events: List<XmlCalendarEvent>? = null,
) {
  @JsonPropertyOrder("day-type", "default-week", "only-show-weekends")
  data class XmlDayTypes(
    @get:JacksonXmlElementWrapper(useWrapping = false)
    @get:JacksonXmlProperty(localName = "day-type")
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    var types: List<XmlDayType>? = null,
    @get:JacksonXmlProperty(localName = "default-week")
    var defaultWeek: XmlDefaultWeek = XmlDefaultWeek(),
    @get:JacksonXmlProperty(localName = "only-show-weekends")
    var onlyShowWeekends: XmlOnlyShowWeekends = XmlOnlyShowWeekends(),
//    @get:JacksonXmlProperty(localName = "overriden-day-types")
//    var overridenDayTypes: String? = null, // TODO: what is that tag?
//    var days: String? = null // TODO: what is that tag?
  ) {
    data class XmlDayType(@get:JacksonXmlProperty(isAttribute = true) var id: String = "")

    @JsonPropertyOrder("id", "name", "sun", "mon", "tue", "wed", "thu", "fri", "sat")
    data class XmlDefaultWeek(
      @get:JacksonXmlProperty(isAttribute = true) var id: String = "1",
      @get:JacksonXmlProperty(isAttribute = true) var name: String = "default",
      @get:JacksonXmlProperty(isAttribute = true) var sun: Int = 0,
      @get:JacksonXmlProperty(isAttribute = true) var mon: Int = 0,
      @get:JacksonXmlProperty(isAttribute = true) var tue: Int = 0,
      @get:JacksonXmlProperty(isAttribute = true) var wed: Int = 0,
      @get:JacksonXmlProperty(isAttribute = true) var thu: Int = 0,
      @get:JacksonXmlProperty(isAttribute = true) var fri: Int = 0,
      @get:JacksonXmlProperty(isAttribute = true) var sat: Int = 0
    )

    data class XmlOnlyShowWeekends(@get:JacksonXmlProperty(isAttribute = true) var value: Boolean = false)
  }

  data class XmlCalendarEvent(
    @get:JacksonXmlProperty(isAttribute = true) var year: String = "",
    @get:JacksonXmlProperty(isAttribute = true) var month: Int = 0,
    @get:JacksonXmlProperty(isAttribute = true) var date: Int = 0,
    @get:JacksonXmlProperty(isAttribute = true) var type: String = "",
    @get:JacksonXmlProperty(isAttribute = true) var color: String? = null,
    @get:JacksonXmlText @get:JacksonXmlCData var value: String? = null
  )
}

@JsonPropertyOrder("taskproperties", "taskproperty")
data class XmlTasks(
  @get:JacksonXmlProperty(isAttribute = true, localName = "empty-milestones") var emptyMilestones: Boolean = true,
  @get:JsonInclude(JsonInclude.Include.NON_NULL)
  @get:JacksonXmlElementWrapper(localName = "taskproperties")
  @get:JacksonXmlProperty(localName = "taskproperty")
  var taskproperties: List<XmlTaskProperty>? = null,

  @get:JacksonXmlElementWrapper(useWrapping = false)
  @get:JsonInclude(JsonInclude.Include.NON_NULL)
  @get:JacksonXmlProperty(localName = "task")
  var tasks: List<XmlTask>? = null
) {
  data class XmlTaskProperty(
    @get:JacksonXmlProperty(isAttribute = true) var id: String = "",
    @get:JacksonXmlProperty(isAttribute = true) var name: String = "",
    @get:JacksonXmlProperty(isAttribute = true) var type: String = "",
    @get:JacksonXmlProperty(isAttribute = true) var valuetype: String = "",
    @get:JacksonXmlProperty(isAttribute = true) var defaultvalue: String? = null,

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    @get:JacksonXmlProperty(localName = "simple-select")
    var simpleSelect: XmlCalculationSimpleSelect? = null
  )

  data class XmlCalculationSimpleSelect(
    @get:JacksonXmlProperty(isAttribute = true) var select: String = "",
  )

  @JsonPropertyOrder(
    "id", "name", "color", "shape", "meeting", "project", "start", "duration", "complete", "thirdDate", "thirdDate-constraint",
    "priority", "webLink", "expand", "cost-manual-value", "cost-calculated", "task", "notes", "depend", "fixed-start"
  )
  data class XmlTask(
    @get:JacksonXmlProperty(isAttribute = true) var id: Int = 0,
    @get:JacksonXmlProperty(isAttribute = true) var uid: String = "",
    @get:JacksonXmlProperty(isAttribute = true) var name: String = "",
    @get:JacksonXmlProperty(isAttribute = true) var color: String? = null,
    @get:JacksonXmlProperty(isAttribute = true) var shape: String? = null,
    @get:JacksonXmlProperty(isAttribute = true, localName = "meeting")
    var isMilestone: Boolean = false,

    @get:JacksonXmlProperty(isAttribute = true, localName = "project")
    var isProjectTask: Boolean = false,

    @get:JacksonXmlProperty(isAttribute = true, localName = "start")
    var startDate: String = "",

    @get:JacksonXmlProperty(isAttribute = true) var duration: Int = 0,

    @get:JacksonXmlProperty(isAttribute = true, localName = "complete")
    var completion: Int = 0,

    @get:JacksonXmlProperty(isAttribute = true, localName = "thirdDate")
    var earliestStartDate: String? = null,

    @get:JacksonXmlProperty(isAttribute = true, localName = "thirdDate-constraint") var thirdDateConstraint: Int? = null,
    @get:JacksonXmlProperty(isAttribute = true) var priority: String? = null,

    @get:JacksonXmlProperty(isAttribute = true) var webLink: String? = null,

    @get:JacksonXmlProperty(isAttribute = true, localName = "expand")
    var isExpanded: Boolean = true,

    @get:JacksonXmlProperty(isAttribute = true, localName = "cost-manual-value")
    var costManualValue: BigDecimal? = null,

    @get:JacksonXmlProperty(isAttribute = true, localName = "cost-calculated")
    var isCostCalculated: Boolean? = null,

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    @get:JacksonXmlCData
    var notes: String? = null,

    @get:JacksonXmlElementWrapper(useWrapping = false)
    @get:JacksonXmlProperty(localName = "depend")
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    var dependencies: List<XmlDependency>? = null,

    @get:JacksonXmlElementWrapper(useWrapping = false)
    @get:JacksonXmlProperty(localName = "customproperty")
    var customPropertyValues: List<XmlCustomProperty> = emptyList(),

    @get:JacksonXmlProperty(localName = "task")
    @get:JacksonXmlElementWrapper(useWrapping = false)
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    var tasks: List<XmlTask>? = null,

    @get:JacksonXmlProperty(isAttribute = true, localName = "fixed-start") var legacyFixedStart: String? = null,
//    @get:JacksonXmlProperty(isAttribute = true) var project: Boolean? = null,
  ) {
    data class XmlDependency(
      @get:JacksonXmlProperty(isAttribute = true) var id: Int = 0,
      @get:JacksonXmlProperty(isAttribute = true) var type: String = "",
      @get:JacksonXmlProperty(isAttribute = true, localName = "difference")
      var lag: Int = 0,
      @get:JacksonXmlProperty(isAttribute = true) var hardness: String = ""
    )

    data class XmlCustomProperty(
      @get:JacksonXmlProperty(isAttribute = true, localName = "taskproperty-id") var propId: String = "",
      @get:JacksonXmlProperty(isAttribute = true) var value: String? = null
    )
  }
}

data class XmlResources(
  @get:JacksonXmlElementWrapper(useWrapping = false)
  @get:JacksonXmlProperty(localName = "custom-property-definition")
  var customProperties: List<XmlCustomPropertyDefinition> = emptyList(),

  @get:JacksonXmlElementWrapper(useWrapping = false)
  @get:JacksonXmlProperty(localName = "resource")
  @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
  var resources: List<XmlResource> = emptyList()
) {
  data class XmlCustomPropertyDefinition(
    @get:JacksonXmlProperty(isAttribute = true) var id: String = "",
    @get:JacksonXmlProperty(isAttribute = true) var name: String = "",
    @get:JacksonXmlProperty(isAttribute = true) var type: String = "",
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    @get:JacksonXmlProperty(isAttribute = true, localName = "default-value") var defaultValue: String? = null,
    @get:JacksonXmlProperty(isAttribute = true, localName = "MSPROJECT_TYPE") var msProjectType: String? = null,
  )

  data class XmlResource(
    @get:JacksonXmlProperty(isAttribute = true) var id: Int = 0,
    @get:JacksonXmlProperty(isAttribute = true) var name: String = "",
    @get:JacksonXmlProperty(isAttribute = true, localName = "function") var role: String = "",
    @get:JacksonXmlProperty(isAttribute = true, localName = "contacts") var email: String = "",
    @get:JacksonXmlProperty(isAttribute = true) var phone: String = "",

    @get:JsonInclude(JsonInclude.Include.NON_NULL) var rate: XmlRate? = null,

    @get:JacksonXmlElementWrapper(useWrapping = false)
    @get:JacksonXmlProperty(localName = "custom-property")
    var props: List<XmlCustomProperty> = emptyList()
  ) {
    data class XmlCustomProperty(
      @get:JacksonXmlProperty(isAttribute = true, localName = "definition-id") var definitionId: String = "",
      @get:JacksonXmlProperty(isAttribute = true) var value: String = ""
    )

    data class XmlRate(
      @get:JacksonXmlProperty(isAttribute = true) var name: String = "",
      @get:JacksonXmlProperty(isAttribute = true) var value: BigDecimal = BigDecimal(0)
    )
  }
}

@JsonPropertyOrder("task-id", "resource-id", "function", "responsible", "load")
data class XmlAllocation(
  @get:JacksonXmlProperty(isAttribute = true, localName = "task-id") var taskId: Int = 0,
  @get:JacksonXmlProperty(isAttribute = true, localName = "resource-id") var resourceId: Int = 0,
  @get:JacksonXmlProperty(isAttribute = true, localName = "function") var role: String? = null,
  @get:JacksonXmlProperty(isAttribute = true, localName = "responsible") var isCoordinator: Boolean = false,
  @get:JacksonXmlProperty(isAttribute = true) var load: Float = 0.0f
)
@JsonPropertyOrder("start", "end", "resourceid")
data class XmlVacation(
  @get:JacksonXmlProperty(isAttribute = true, localName = "start") var startDate: String = "",
  @get:JacksonXmlProperty(isAttribute = true, localName = "end") var endDate: String = "",
  @get:JacksonXmlProperty(isAttribute = true, localName = "resourceid") var resourceid: Int = 0
)

data class XmlRoles(
  @get:JacksonXmlProperty(isAttribute = true, localName = "roleset-name")
  var rolesetName: String? = null,

  @get:JacksonXmlElementWrapper(useWrapping = false)
  @get:JacksonXmlProperty(localName = "role")
  @get:JsonInclude(JsonInclude.Include.NON_NULL)
  var roles: List<XmlRole>? = null
) {
  data class XmlRole(
    @get:JacksonXmlProperty(isAttribute = true) var id: String = "",
    @get:JacksonXmlProperty(isAttribute = true) var name: String = ""
  )
}

data class XmlBaselineList(
  @get:JacksonXmlElementWrapper(useWrapping = false)
  @get:JacksonXmlProperty(localName = "previous-tasks")
  @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
  var baselines: List<XmlBaseline> = emptyList()
) {
  data class XmlBaseline(
    @get:JacksonXmlProperty(isAttribute = true)
    var name: String = "",

    @get:JacksonXmlElementWrapper(useWrapping = false)
    @get:JacksonXmlProperty(localName = "previous-task")
    var tasks: List<XmlBaselineTask>? = null
  )

  data class XmlBaselineTask(
    @get:JacksonXmlProperty(isAttribute = true) var id: Int = 0,
    @get:JacksonXmlProperty(isAttribute = true, localName = "start") var startDate: String = "",
    @get:JacksonXmlProperty(isAttribute = true) var duration: Int = 0,
    @get:JacksonXmlProperty(isAttribute = true, localName = "meeting") var isMilestone: Boolean = false,
    @get:JacksonXmlProperty(isAttribute = true, localName = "super") var isSummaryTask: Boolean = false
  )
}

fun XmlProject.toXml(): String = xmlMapper.writeValueAsString(this)

fun parseXmlProject(xml: String): XmlProject = xmlMapper.readValue(xml, XmlProject::class.java)

fun XmlProject.collectTasksDepthFirst(): List<XmlTasks.XmlTask> {
  val result = mutableListOf<XmlTasks.XmlTask>()
  var queue = this.tasks.tasks?.toMutableList() ?: mutableListOf()
  while (queue.isNotEmpty()) {
    queue.removeFirst().also {
      result.add(it)
      it.tasks?.toMutableList()?.also {
        queue = (it + queue).toMutableList()
      }
    }
  }
  return result
}

fun XmlProject.walkTasksDepthFirst(visitor: (XmlTasks.XmlTask?, XmlTasks.XmlTask)->Boolean) {
  if (!this.tasks.tasks.isNullOrEmpty()) {
    doWalkTasksDepthFirst(null, this.tasks.tasks!!, visitor)
  }
}

private fun doWalkTasksDepthFirst(root: XmlTasks.XmlTask?, children: List<XmlTasks.XmlTask>, visitor: (XmlTasks.XmlTask?, XmlTasks.XmlTask)->Boolean) {
  children.forEach {
    if (visitor(root, it) && !it.tasks.isNullOrEmpty()) {
      doWalkTasksDepthFirst(it, it.tasks!!, visitor)
    }
  }
}
private val xmlMapper = XmlMapper().also {
  it.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
  it.enable(SerializationFeature.INDENT_OUTPUT)
  it.configOverride(List::class.java).setterInfo = JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY)
  it.configure(FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL, true)
  it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}
