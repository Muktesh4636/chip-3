package com.transactionhub.ui.more

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.transactionhub.R
import com.transactionhub.data.api.ApiService
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch
import java.util.*

data class TaskItem(
    val id: Int,
    val title: String,
    val description: String,
    val priority: String,
    val status: String,
    val dueDate: String,
    val assignedTo: String,
    val category: String
)

class TaskAdapter(private val tasks: List<TaskItem>) :
    RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.taskTitle)
        val description: TextView = view.findViewById(R.id.taskDescription)
        val priority: TextView = view.findViewById(R.id.taskPriority)
        val status: TextView = view.findViewById(R.id.taskStatus)
        val dueDate: TextView = view.findViewById(R.id.taskDueDate)
        val assignedTo: TextView = view.findViewById(R.id.taskAssignedTo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.title.text = task.title
        holder.description.text = task.description
        holder.priority.text = "Priority: ${task.priority}"
        holder.status.text = "Status: ${task.status}"
        holder.dueDate.text = "Due: ${task.dueDate}"
        holder.assignedTo.text = "Assigned: ${task.assignedTo}"
    }

    override fun getItemCount() = tasks.size
}

class TaskCalendarFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService
    private lateinit var taskAdapter: TaskAdapter
    private val tasks = mutableListOf<TaskItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_task_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.taskCalendarTitle).text = "Task Management & Calendar"

        setupTaskList(view)
        loadTaskData(view)
        setupTaskControls(view)
    }

    private fun setupTaskList(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.taskRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        taskAdapter = TaskAdapter(tasks)
        recyclerView.adapter = taskAdapter
    }

    private fun loadTaskData(view: View) {
        // Sample tasks - in real app this would come from API
        tasks.addAll(listOf(
            TaskItem(1, "Review Client Portfolio", "Conduct quarterly portfolio review for high-value clients", "High", "In Progress", "2025-01-25", "Portfolio Manager", "Client Management"),
            TaskItem(2, "Process Tax Documents", "Prepare and submit Q4 tax documentation", "High", "Pending", "2025-01-30", "Compliance Officer", "Compliance"),
            TaskItem(3, "Client Meeting Preparation", "Prepare presentation for client meeting", "Medium", "In Progress", "2025-01-20", "Account Manager", "Client Relations"),
            TaskItem(4, "Risk Assessment Review", "Review and update risk assessment models", "Medium", "Completed", "2025-01-18", "Risk Analyst", "Risk Management"),
            TaskItem(5, "System Maintenance", "Perform scheduled system maintenance and updates", "Low", "Scheduled", "2025-01-28", "IT Team", "Operations"),
            TaskItem(6, "Staff Training Session", "Conduct quarterly staff training on new regulations", "Medium", "Planning", "2025-02-05", "HR Manager", "Training"),
            TaskItem(7, "Financial Report Generation", "Generate monthly financial performance report", "High", "In Progress", "2025-01-31", "Finance Team", "Reporting"),
            TaskItem(8, "Client Onboarding", "Complete onboarding process for new institutional client", "High", "In Progress", "2025-01-22", "Client Services", "Onboarding"),
            TaskItem(9, "Audit Preparation", "Prepare documents for annual external audit", "High", "Planning", "2025-02-15", "Compliance Officer", "Audit"),
            TaskItem(10, "Market Research", "Conduct research on emerging market trends", "Medium", "Ongoing", "2025-03-01", "Research Team", "Market Analysis")
        ))

        // Task Statistics
        view.findViewById<TextView>(R.id.taskStats).text = """
        📊 TASK DASHBOARD

        🎯 Total Tasks: ${tasks.size}
        ✅ Completed: ${tasks.count { it.status == "Completed" }}
        🔄 In Progress: ${tasks.count { it.status == "In Progress" }}
        ⏳ Pending: ${tasks.count { it.status == "Pending" }}
        📅 Overdue: ${tasks.count { it.dueDate < "2025-01-20" && it.status != "Completed" }}

        🔥 Priority Breakdown:
        🔴 High Priority: ${tasks.count { it.priority == "High" }}
        🟡 Medium Priority: ${tasks.count { it.priority == "Medium" }}
        🟢 Low Priority: ${tasks.count { it.priority == "Low" }}

        👥 Team Workload:
        Portfolio Manager: ${tasks.count { it.assignedTo == "Portfolio Manager" }}
        Compliance Officer: ${tasks.count { it.assignedTo == "Compliance Officer" }}
        Account Manager: ${tasks.count { it.assignedTo == "Account Manager" }}
        IT Team: ${tasks.count { it.assignedTo == "IT Team" }}
        Finance Team: ${tasks.count { it.assignedTo == "Finance Team" }}
        """.trimIndent()

        // Upcoming Deadlines
        view.findViewById<TextView>(R.id.upcomingDeadlines).text = """
        ⏰ UPCOMING DEADLINES

        🔥 Today (Jan 19):
        • Client Meeting Preparation - Account Manager
        • Risk Assessment Review - Risk Analyst

        📅 This Week (Jan 20-25):
        • Review Client Portfolio - Portfolio Manager (Jan 25)
        • Process Tax Documents - Compliance Officer (Jan 30)
        • Client Onboarding - Client Services (Jan 22)

        📆 Next Week (Jan 26-31):
        • System Maintenance - IT Team (Jan 28)
        • Financial Report Generation - Finance Team (Jan 31)

        📋 This Month (Feb):
        • Staff Training Session - HR Manager (Feb 5)
        • Audit Preparation - Compliance Officer (Feb 15)
        • Market Research - Research Team (Mar 1)
        """.trimIndent()

        // Calendar Events
        view.findViewById<TextView>(R.id.calendarEvents).text = """
        📅 CALENDAR EVENTS

        January 2025 Calendar:

        📊 19 Jan (Today):
        9:00 AM - Daily Standup Meeting
        2:00 PM - Client Portfolio Review
        4:00 PM - Risk Assessment Session

        💼 20 Jan (Tomorrow):
        10:00 AM - Team Strategy Meeting
        3:00 PM - Client Presentation Prep

        🎯 22 Jan:
        11:00 AM - New Client Onboarding
        2:00 PM - Compliance Training

        📈 25 Jan:
        9:00 AM - Quarterly Portfolio Review
        1:00 PM - Performance Analysis

        🔧 28 Jan:
        8:00 AM - System Maintenance Window
        2:00 PM - IT Infrastructure Review

        💰 30 Jan:
        10:00 AM - Tax Documentation Deadline
        3:00 PM - Financial Reporting

        February 2025 Preview:
        🎓 5 Feb - Staff Training Day
        📋 15 Feb - Audit Preparation Kickoff
        📊 Q1 Performance Review
        """.trimIndent()

        taskAdapter.notifyDataSetChanged()
    }

    private fun setupTaskControls(view: View) {
        view.findViewById<Button>(R.id.btnCreateTask).setOnClickListener {
            createNewTask()
        }

        view.findViewById<Button>(R.id.btnViewCalendar).setOnClickListener {
            showCalendarView()
        }

        view.findViewById<Button>(R.id.btnTaskFilters).setOnClickListener {
            showTaskFilters()
        }

        view.findViewById<Button>(R.id.btnTaskReports).setOnClickListener {
            showTaskReports()
        }

        view.findViewById<Button>(R.id.btnScheduleMeeting).setOnClickListener {
            scheduleMeeting()
        }
    }

    private fun createNewTask() {
        val taskTypes = arrayOf(
            "Client Management Task",
            "Compliance & Regulatory",
            "Financial Reporting",
            "IT & Technical",
            "Training & Development",
            "Risk Assessment",
            "Operations & Maintenance",
            "Audit & Documentation",
            "Market Research",
            "Strategic Planning"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Create New Task")
            .setItems(taskTypes) { _, which ->
                val selectedType = taskTypes[which]
                Toast.makeText(context, "Creating $selectedType...", Toast.LENGTH_SHORT).show()

                lifecycleScope.launch {
                    kotlinx.coroutines.delay(1000)
                    Toast.makeText(context, "$selectedType created successfully!", Toast.LENGTH_SHORT).show()
                }
            }
            .setPositiveButton("Custom Task", null)
            .show()
    }

    private fun showCalendarView() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(),
            { _, year, month, day ->
                val selectedDate = "$year-${month+1}-$day"
                Toast.makeText(context, "Viewing calendar for $selectedDate", Toast.LENGTH_SHORT).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTaskFilters() {
        val filters = arrayOf(
            "All Tasks",
            "My Tasks",
            "High Priority",
            "Overdue Tasks",
            "Completed Today",
            "Due This Week",
            "By Team Member",
            "By Category",
            "By Status"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Filter Tasks")
            .setItems(filters) { _, which ->
                val selectedFilter = filters[which]
                Toast.makeText(context, "Applying filter: $selectedFilter", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showTaskReports() {
        val report = """
        TASK MANAGEMENT REPORT

        📊 EXECUTION METRICS:
        • Tasks Completed This Month: 47
        • Average Completion Time: 3.2 days
        • On-Time Completion Rate: 89%
        • Task Creation Rate: 12 tasks/week

        👥 TEAM PERFORMANCE:
        • Most Productive: Account Manager (15 tasks)
        • Fastest Resolution: IT Team (1.8 days avg)
        • Highest Quality: Compliance Officer (98% success)

        🎯 EFFICIENCY METRICS:
        • Time Saved with Automation: 25 hours/week
        • Process Improvement Rate: +34% QoQ
        • Client Satisfaction Impact: +18%
        • Cost Reduction: ₹45,000/month

        📈 TREND ANALYSIS:
        • Task Volume: +22% MoM
        • Completion Speed: +15% improvement
        • Quality Metrics: +8% improvement
        • Team Productivity: +28% increase

        🔍 INSIGHTS & RECOMMENDATIONS:
        • Implement task priority scoring system
        • Add automated task assignment based on workload
        • Create task templates for common activities
        • Enhance mobile task notification system
        • Integrate with calendar for better scheduling
        • Add task dependency management
        • Implement time tracking for tasks
        • Create dashboard for real-time task monitoring
        """.trimIndent()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Task Management Report")
            .setMessage(report)
            .setPositiveButton("Export Report", null)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun scheduleMeeting() {
        val meetingTypes = arrayOf(
            "Team Standup Meeting",
            "Client Review Meeting",
            "Strategy Planning Session",
            "Training Workshop",
            "Compliance Review",
            "Performance Review",
            "Project Kickoff",
            "Stakeholder Meeting",
            "Department Meeting",
            "All-Hands Meeting"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Schedule Meeting")
            .setItems(meetingTypes) { _, which ->
                val selectedType = meetingTypes[which]
                Toast.makeText(context, "Scheduling $selectedType...", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Custom Meeting", null)
            .show()
    }
}