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

class HRManagementFragment : Fragment() {
    private lateinit var prefManager: PrefManager
    private lateinit var apiService: ApiService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_hr_management, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        apiService = ApiClient.apiService

        view.findViewById<TextView>(R.id.hrTitle).text = "HR Management"

        loadHRData(view)
        setupHRControls(view)
    }

    private fun loadHRData(view: View) {
        // Employee Summary
        view.findViewById<TextView>(R.id.employeeSummary).text = """
        👥 EMPLOYEE DIRECTORY SUMMARY

        👤 Total Staff: 45 employees
        • Executive: 3 | Management: 8
        • Operations: 12 | Sales: 10
        • Engineering: 7 | Support: 5

        📊 Department Breakdown:
        • Finance: 15% | Technology: 25%
        • Business Development: 35% | Legal: 15%
        • HR/Admin: 10%

        📈 Attrition Rate: 2.1% (Annual)
        😊 Employee Satisfaction: 4.6/5
        """.trimIndent()

        // Attendance & Leave
        view.findViewById<TextView>(R.id.attendanceLeave).text = """
        📅 ATTENDANCE & LEAVE (Jan 2025)

        ✅ Present Today: 42 employees (93%)
        🏠 Work From Home: 8 employees
        🌴 On Leave: 3 employees
        🚪 Late Arrival: 1 employee

        📋 Leave Requests Pending: 5
        📊 Avg. Monthly Absenteeism: 1.2%
        ⏱️ Avg. Work Hours: 8.2 hrs/day

        🎯 Next Team Outing: Feb 10, 2025
        """.trimIndent()

        // Payroll & Compensation
        view.findViewById<TextView>(R.id.payrollCompensation).text = """
        💰 PAYROLL & COMPENSATION

        💵 Monthly Payroll: ₹32,45,000
        💵 Avg. Salary: ₹72,111
        💵 Bonus Distributed: ₹4,50,000 (Q4)

        📈 Salary Growth: +8.5% (Yearly avg)
        🛡️ Insurance Coverage: 100% staff
        🏦 PF Compliance: 100%

        📅 Next Pay Date: Jan 31, 2025
        ✅ Payroll Processing: Ready
        """.trimIndent()

        // Recruitment & Onboarding
        view.findViewById<TextView>(R.id.recruitmentOnboarding).text = """
        🤝 RECRUITMENT & ONBOARDING

        🔥 Active Openings: 5 positions
        • Senior Portfolio Manager
        • Full-Stack Developer
        • Compliance Analyst
        • Sales Executive (2)

        📋 Candidates in Pipeline: 23
        🎯 Offers Extended: 2
        ✅ Onboarding This Week: 1 (Sarah Jain)

        ⏱️ Avg. Time to Hire: 24 days
        """.trimIndent()
    }

    private fun setupHRControls(view: View) {
        view.findViewById<Button>(R.id.btnAddEmployee).setOnClickListener {
            addNewEmployee()
        }

        view.findViewById<Button>(R.id.btnManageLeave).setOnClickListener {
            manageLeave()
        }

        view.findViewById<Button>(R.id.btnProcessPayroll).setOnClickListener {
            processPayroll()
        }

        view.findViewById<Button>(R.id.btnHRReports).setOnClickListener {
            showHRReports()
        }

        view.findViewById<Button>(R.id.btnHRSettings).setOnClickListener {
            hrSettings()
        }
    }

    private fun addNewEmployee() {
        val empTypes = arrayOf(
            "Full-Time Employee",
            "Part-Time Employee",
            "Contractor/Consultant",
            "Intern",
            "Remote Worker"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Add New Employee")
            .setItems(empTypes) { _, which ->
                Toast.makeText(context, "Opening registration for ${empTypes[which]}...", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Quick Add", null)
            .show()
    }

    private fun manageLeave() {
        val leaveOptions = arrayOf(
            "View All Applications (5)",
            "My Leave Status",
            "Holiday Calendar 2025",
            "Leave Policy Document",
            "Attendance Correction",
            "Work from Home Request"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Leave & Attendance")
            .setItems(leaveOptions) { _, which ->
                Toast.makeText(context, "Opening ${leaveOptions[which]}...", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun processPayroll() {
        val payrollSteps = arrayOf(
            "Verify Attendance Data",
            "Calculate Bonuses & Incentives",
            "Deduct Taxes/PF/Insurance",
            "Generate Pay Slips",
            "Initiate Bank Transfer",
            "Compliance Documentation"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Payroll Processing")
            .setItems(payrollSteps) { _, which ->
                Toast.makeText(context, "Starting ${payrollSteps[which]}...", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Auto-Process All", null)
            .show()
    }

    private fun showHRReports() {
        val report = """
        HR PERFORMANCE & ANALYTICS

        📊 HEADCOUNT TRENDS:
        • Jan 2024: 32 staff
        • Jan 2025: 45 staff (+40% growth)
        • Target 2025: 60 staff

        👥 EMPLOYEE ENGAGEMENT:
        • Avg. Tenure: 2.4 years
        • Promotion Rate: 12%
        • Internal Transfer Rate: 5%
        • Recognition Awards: 8 this month

        💰 COMPENSATION ANALYTICS:
        • Labor Cost Ratio: 28.4%
        • Competitiveness Index: 92% (vs market)
        • Bonus/Revenue Ratio: 4.2%

        🚀 STRATEGIC RECOMMENDATIONS:
        • Expand engineering team in Bangalore
        • Implement leadership development program
        • Update remote work policy for 2025
        • Enhance mental wellness benefits
        """.trimIndent()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("HR Analytics")
            .setMessage(report)
            .setPositiveButton("Export Report", null)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun hrSettings() {
        val settings = arrayOf(
            "Company Policies",
            "Role & Permission Matrix",
            "Salary Structures",
            "Benefit Packages",
            "Performance Review Cycles",
            "HR System Integrations"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("HR Configuration")
            .setItems(settings) { _, which ->
                Toast.makeText(context, "Opening ${settings[which]} settings...", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}