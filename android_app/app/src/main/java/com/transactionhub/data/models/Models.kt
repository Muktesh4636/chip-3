package com.transactionhub.data.models

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user_id: Int,
    val username: String
)

data class DashboardResponse(
    val total_clients: Int,
    val total_exchanges: Int,
    val total_accounts: Int,
    val total_funding: Long,
    val total_balance: Long,
    val total_pnl: Long,
    val total_my_share: Long,
    val currency: String
)

data class Client(
    val id: Int,
    val name: String,
    val code: String? = null,
    val referred_by: String? = null,
    val is_company_client: Boolean = false
)

data class Exchange(
    val id: Int,
    val name: String,
    val version_name: String?,
    val code: String?
)

data class Account(
    val id: Int,
    val client: Int,
    val client_name: String,
    val exchange: Int,
    val exchange_name: String,
    val funding: Long,
    val exchange_balance: Long,
    val pnl: Long,
    val my_share: Long,
    val loss_share_percentage: Int,
    val profit_share_percentage: Int
)

data class Transaction(
    val id: Int,
    val client_exchange: Int,
    val client_name: String,
    val exchange_name: String,
    val date: String,
    val type: String,
    val type_display: String,
    val amount: Long,
    val funding_before: Long?,
    val funding_after: Long?,
    val exchange_balance_before: Long?,
    val exchange_balance_after: Long?,
    val sequence_no: Int,
    val notes: String?
)

data class PendingPaymentItem(
    val account_id: Int,
    val client_name: String,
    val exchange_name: String,
    val pnl: Long,
    val my_share: Long,
    val type: String
)

data class PendingPaymentsResponse(
    val pending_payments: List<PendingPaymentItem>,
    val total_to_receive: Long,
    val total_to_pay: Long,
    val currency: String
)

data class EditTransactionRequest(
    val amount: Long,
    val notes: String? = null
)

data class BulkTransactionAction(
    val action: String, // "delete", "export", "mark_paid"
    val transaction_ids: List<Int>
)

data class UserProfile(
    val id: Int,
    val username: String,
    val email: String,
    val first_name: String? = null,
    val last_name: String? = null,
    val date_joined: String,
    val last_login: String? = null
)

data class ChangePasswordRequest(
    val old_password: String,
    val new_password: String,
    val confirm_password: String
)

data class AuditLogEntry(
    val id: Int,
    val timestamp: String,
    val action: String,
    val details: String,
    val ip_address: String? = null,
    val user_agent: String? = null
)

data class ChartDataPoint(
    val date: String,
    val value: Double,
    val label: String? = null
)

data class PerformanceChart(
    val pnl_over_time: List<ChartDataPoint>,
    val funding_over_time: List<ChartDataPoint>,
    val balance_over_time: List<ChartDataPoint>,
    val share_over_time: List<ChartDataPoint>
)
