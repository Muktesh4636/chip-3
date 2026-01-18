package com.transactionhub.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.transactionhub.R
import com.transactionhub.data.api.ApiService
import com.transactionhub.utils.ApiClient
import com.transactionhub.utils.PrefManager
import kotlinx.coroutines.launch

class ProjectManagementFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_project_management, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.projectTitle).text = "Project Management"

        loadProjectData(view)
        setupProjectControls(view)
    }

    private fun loadProjectData(view: View) {
        // Active Projects
        view.findViewById<TextView>(R.id.activeProjects).text = """
        🚀 ACTIVE PROJECTS SUMMARY

        ✅ Q1 Financial Audit
        • Status: In Progress | Progress: 65%
        • Deadline: Feb 15 | Team: Compliance

        ✅ New Client Portal V2
        • Status: Planning | Progress: 15%
        • Deadline: Mar 30 | Team: Product

        ✅ Mobile App Optimization
        • Status: Execution | Progress: 45%
        • Deadline: Feb 28 | Team: Engineering

        ✅ Annual Strategy Review
        • Status: In Progress | Progress: 80%
        • Deadline: Jan 31 | Team: Executive
        """.trimIndent()

        // Key Milestones
        view.findViewById<TextView>(R.id.keyMilestones).text = """
        🏁 KEY MILESTONES

        🔥 Today:
        • Finalize Q4 Tax Submissions
        • Complete Beta Testing for V2

        🗓️ Next Week:
        • Client Data Migration Phase 1
        • Security Patch Implementation
        • Team Strategy Session

        📋 This Month:
        • Infrastructure Upgrade Completion
        • New Office Setup Finalization
        """.trimIndent()

        // Team Capacity
        view.findViewById<TextView>(R.id.teamCapacity).text = """
        👥 TEAM CAPACITY & WORKLOAD

        📊 Engineering: 85% occupied
        📊 Compliance: 92% occupied
        📊 Sales: 65% occupied
        📊 Support: 78% occupied
        📊 Executive: 95% occupied

        🎯 Top Performers:
        1. Engineering Team (Mobile)
        2. Compliance Review Board
        3. Strategic Sales Group
        """.trimIndent()

        // Resource Allocation
        view.findViewById<TextView>(R.id.resourceAllocation).text = """
        💰 RESOURCE ALLOCATION

        💵 Project Budget: ₹85,00,000
        💵 Spent to Date: ₹32,45,000
        💵 Remaining: ₹52,55,000

        📈 Burn Rate: ₹8,50,000/month
        ⏱️ Projected Finish: 6.2 months

        ⚠️ Alert: Engineering budget over by 5%
        ✅ Success: Marketing spend under by 12%
        """.trimIndent()
    }

    private fun setupProjectControls(view: View) {
        view.findViewById<Button>(R.id.btnCreateProject).setOnClickListener {
            createNewProject()
        }

        view.findViewById<Button>(R.id.btnViewTimeline).setOnClickListener {
            viewProjectTimeline()
        }

        view.findViewById<Button>(R.id.btnManageTasks).setOnClickListener {
            manageProjectTasks()
        }

        view.findViewById<Button>(R.id.btnProjectReports).setOnClickListener {
            showProjectReports()
        }

        view.findViewById<Button>(R.id.btnProjectSettings).setOnClickListener {
            projectSettings()
        }
    }

    private fun createNewProject() {
        val projectTypes = arrayOf(
            "Software Development",
            "Infrastructure Upgrade",
            "Compliance Audit",
            "Marketing Campaign",
            "Organizational Change",
            "Financial Restructuring",
            "Product Launch"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Create New Project")
            .setItems(projectTypes) { _, which ->
                Toast.makeText(context, "Initializing ${projectTypes[which]}...", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Custom Project", null)
            .show()
    }

    private fun viewProjectTimeline() {
        val timelines = arrayOf(
            "Overall Roadmap",
            "Monthly Sprint View",
            "Weekly Gantt Chart",
            "Critical Path Analysis",
            "Dependency Map"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Project Timelines")
            .setItems(timelines) { _, which ->
                Toast.makeText(context, "Loading ${timelines[which]}...", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun manageProjectTasks() {
        val taskCategories = arrayOf(
            "Pending Tasks (45)",
            "Active Tasks (12)",
            "Review Needed (8)",
            "Completed Tasks (156)",
            "Blocked Tasks (3)"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Task Management")
            .setItems(taskCategories) { _, which ->
                Toast.makeText(context, "Opening ${taskCategories[which]}...", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Create Task", null)
            .show()
    }

    private fun showProjectReports() {
        val report = """
        PROJECT PERFORMANCE DASHBOARD

        📊 KPI SUMMARY:
        • On-Time Delivery: 89%
        • Budget Adherence: 94%
        • Resource Utilization: 82%
        • Stakeholder Satisfaction: 4.7/5

        🔥 CRITICAL ISSUES:
        • Mobile API Latency (+200ms)
        • Compliance Document Backlog (12 items)
        • Marketing Asset Delay (3 days)

        💰 FINANCIAL HEALTH:
        • Total ROI: +34%
        • Cost Savings: ₹12,50,000
        • Budget Variance: -2.3%

        🚀 UPCOMING LAUNCHES:
        • V2 Portal: Mar 15
        • Analytics Suite: Apr 30
        • CRM Integration: May 15
        """.trimIndent()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Project Analytics")
            .setMessage(report)
            .setPositiveButton("Export Report", null)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun projectSettings() {
        val settings = arrayOf(
            "Methodology Configuration",
            "Team Permissions",
            "Notification Thresholds",
            "Integration Hooks",
            "Template Management",
            "Archive Policies"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Project Settings")
            .setItems(settings) { _, which ->
                Toast.makeText(context, "Opening ${settings[which]}...", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}