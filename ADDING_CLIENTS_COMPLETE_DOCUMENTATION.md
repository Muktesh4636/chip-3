# ADDING CLIENTS - COMPLETE DOCUMENTATION

**Version:** 1.0 (Complete & Comprehensive)  
**Status:** Production Ready  
**Last Updated:** January 2026

---

## TABLE OF CONTENTS

1. [System Overview](#system-overview)
2. [Database Schema](#database-schema)
3. [UI Components](#ui-components)
4. [View Logic](#view-logic)
5. [URL Routing](#url-routing)
6. [Validation Rules](#validation-rules)
7. [Data Flow](#data-flow)
8. [Edge Cases & Scenarios](#edge-cases--scenarios)
9. [Code Reference](#code-reference)
10. [Testing Guide](#testing-guide)

---

## SYSTEM OVERVIEW

The **Adding Clients System** allows authenticated users to create new client profiles in the system. Clients can be:

- **Personal Clients** (`my_client_create`): Clients assigned to the logged-in user
- **Company Clients** (`client_create`): Clients marked as company clients (can be assigned to any user)

### Key Features

- ✅ Client name (required)
- ✅ Client code (optional, unique)
- ✅ Referred by (optional)
- ✅ Company client flag (optional)
- ✅ User assignment (automatic for personal clients)
- ✅ Timestamp tracking (created_at, updated_at)
- ✅ Success/error messaging
- ✅ Form validation

### Two Creation Paths

1. **Standard Client Creation** (`client_create`)
   - Full form with company client checkbox
   - Can create company or personal clients
   - URL: `/clients/create/`

2. **My Client Creation** (`my_client_create`)
   - Simplified form (no company client checkbox)
   - Always creates personal clients
   - URL: `/clients/create/my/` (if exists)

---

## DATABASE SCHEMA

### Client Model

**Location:** `core/models.py` (lines 20-34)

**Table Name:** `core_client`

**Fields:**

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BigAutoField | Primary Key | Auto-generated unique ID |
| `name` | CharField(200) | NOT NULL | Client name (required) |
| `code` | CharField(50) | NULL, UNIQUE | Client code (optional, unique if provided) |
| `referred_by` | CharField(200) | NULL | Name of referrer (optional) |
| `is_company_client` | BooleanField | Default: False | Company client flag |
| `user` | ForeignKey(User) | NULL, SET_NULL | Assigned user (can be null) |
| `created_at` | DateTimeField | Auto-now-add | Creation timestamp |
| `updated_at` | DateTimeField | Auto-now | Last update timestamp |

### Model Definition

```python
class Client(TimeStampedModel):
    """
    Client entity - trades on exchange, receives FULL profit, pays FULL loss.
    """
    name = models.CharField(max_length=200)
    code = models.CharField(max_length=50, blank=True, null=True, unique=True)
    referred_by = models.CharField(max_length=200, blank=True, null=True)
    is_company_client = models.BooleanField(default=False)
    user = models.ForeignKey(User, on_delete=models.SET_NULL, null=True, blank=True, related_name='clients')
    
    class Meta:
        ordering = ['name']
    
    def __str__(self):
        return self.name
```

### Database Constraints

1. **Primary Key:** `id` (auto-increment)
2. **Unique Constraint:** `code` (if provided, must be unique)
3. **Foreign Key:** `user` → `auth_user` (SET_NULL on delete)
4. **Index:** Default index on `id`
5. **Ordering:** Default ordering by `name` (ascending)

### TimeStampedModel Base

**Location:** `core/models.py` (lines 11-17)

```python
class TimeStampedModel(models.Model):
    """Abstract base to track created/updated timestamps."""
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        abstract = True
```

**Inherited Fields:**
- `created_at`: Set automatically on creation
- `updated_at`: Updated automatically on save

---

## UI COMPONENTS

### Template 1: Standard Client Creation

**File:** `core/templates/core/clients/create.html`

**URL:** `/clients/create/`

**Features:**
- Full form with all fields
- Company client checkbox
- Success/error message display
- Cancel button

**Form Fields:**

1. **Client Name** (Required)
   - Type: Text input
   - HTML5 validation: `required`
   - Max length: 200 characters
   - Label: "Client Name *"

2. **Client Code** (Optional)
   - Type: Text input
   - Max length: 50 characters
   - Label: "Client Code (Optional)"
   - Placeholder: "Leave blank if not needed"

3. **Referred By** (Optional)
   - Type: Text input
   - Max length: 200 characters
   - Label: "Referred By (Optional)"
   - Placeholder: "Name of person who referred this client"

4. **Company Client** (Optional)
   - Type: Checkbox
   - Label: "Company Client"
   - Help text: "Check this if this is a company client. Leave unchecked for your personal clients."

**Template Structure:**

```html
{% extends "core/base.html" %}

{% block title %}Add Client · Transaction Hub{% endblock %}
{% block page_title %}Add Client{% endblock %}
{% block page_subtitle %}Create a new client profile{% endblock %}

{% block content %}
<div class="card" style="max-width: 420px;">
    <!-- Messages -->
    {% if messages %}
        <!-- Display success/error messages -->
    {% endif %}
    
    <!-- Form -->
    <form method="post">
        {% csrf_token %}
        <!-- Form fields -->
        <!-- Submit/Cancel buttons -->
    </form>
</div>
{% endblock %}
```

**Styling:**
- Card container: `max-width: 420px`
- Form rows: `.form-row` class
- Field labels: `.field-label` class
- Field inputs: `.field-input` class
- Buttons: `.btn`, `.btn-primary` classes

---

### Template 2: My Client Creation

**File:** `core/templates/core/clients/create_my.html`

**URL:** `/clients/create/my/` (if route exists)

**Features:**
- Simplified form (no company client checkbox)
- Always creates personal clients
- Same field structure as standard form

**Form Fields:**

1. **Client Name** (Required)
   - Same as standard form

2. **Client Code** (Optional)
   - Same as standard form

3. **Referred By** (Optional)
   - Same as standard form

**Differences from Standard Form:**
- ❌ No "Company Client" checkbox
- ✅ Always sets `is_company_client = False`
- ✅ Always assigns to `request.user`

---

### Message Display

**Success Message:**
```html
<div style="background: #d1fae5; color: #065f46; border: 1px solid #10b981;">
    Client '{name}' has been created successfully.
</div>
```

**Error Message:**
```html
<div style="background: #fee2e2; color: #991b1b; border: 1px solid #dc2626;">
    Error message here
</div>
```

**Info Message:**
```html
<div style="background: #dbeafe; color: #1e40af; border: 1px solid #3b82f6;">
    Info message here
</div>
```

---

## VIEW LOGIC

### View 1: Standard Client Creation

**Function:** `client_create`

**Location:** `core/views.py` (lines 644-667)

**Decorator:** `@login_required`

**Method:** GET and POST

**Flow:**

```
1. User navigates to /clients/create/
2. GET request → Show form
3. User fills form and submits
4. POST request → Process form
5. Validate name (required)
6. Create Client object
7. Redirect to client list with success message
```

**Code:**

```python
@login_required
def client_create(request):
    """Create a new client"""
    if request.method == "POST":
        # Extract form data
        name = request.POST.get("name", "").strip()
        code = request.POST.get("code", "").strip()
        referred_by = request.POST.get("referred_by", "").strip()
        is_company_client = request.POST.get("is_company_client") == "on"
        
        # Validate name
        if name:
            # Create client
            Client.objects.create(
                user=request.user,
                name=name,
                code=code if code else None,
                referred_by=referred_by if referred_by else None,
                is_company_client=is_company_client,
            )
            # Success message
            messages.success(request, f"Client '{name}' has been created successfully.")
            return redirect(reverse("client_list"))
        else:
            # Error: name required
            messages.error(request, "Client name is required.")
    
    # GET request or validation failed
    return render(request, "core/clients/create.html")
```

**Key Steps:**

1. **Extract Form Data:**
   ```python
   name = request.POST.get("name", "").strip()
   code = request.POST.get("code", "").strip()
   referred_by = request.POST.get("referred_by", "").strip()
   is_company_client = request.POST.get("is_company_client") == "on"
   ```

2. **Validate Name:**
   ```python
   if name:
       # Proceed with creation
   else:
       # Show error
   ```

3. **Create Client:**
   ```python
   Client.objects.create(
       user=request.user,  # Assign to logged-in user
       name=name,
       code=code if code else None,  # Convert empty string to None
       referred_by=referred_by if referred_by else None,
       is_company_client=is_company_client,
   )
   ```

4. **Success Redirect:**
   ```python
   messages.success(request, f"Client '{name}' has been created successfully.")
   return redirect(reverse("client_list"))
   ```

---

### View 2: My Client Creation

**Function:** `my_client_create`

**Location:** `core/views.py` (lines 676-697)

**Decorator:** `@login_required`

**Method:** GET and POST

**Flow:**

```
1. User navigates to /clients/create/my/
2. GET request → Show simplified form
3. User fills form and submits
4. POST request → Process form
5. Validate name (required)
6. Create Client object (always personal)
7. Redirect to client list
```

**Code:**

```python
@login_required
def my_client_create(request):
    """Create a my (personal) client"""
    if request.method == "POST":
        from django.shortcuts import redirect
        from django.urls import reverse
        from core.models import Client

        name = request.POST.get("name", "").strip()
        code = request.POST.get("code", "").strip()
        referred_by = request.POST.get("referred_by", "").strip()
        
        if name:
            Client.objects.create(
                user=request.user,  # Always assigned to logged-in user
                name=name,
                code=code if code else None,
                referred_by=referred_by if referred_by else None,
                # is_company_client defaults to False
            )
            return redirect("client_list")

    return render(request, "core/clients/create_my.html")
```

**Differences from Standard Creation:**

- ❌ No `is_company_client` handling (always False)
- ✅ Always assigns to `request.user`
- ✅ Simpler form (no company checkbox)

---

## URL ROUTING

### URL Patterns

**File:** `core/urls.py`

**Patterns:**

```python
urlpatterns = [
    # ... other patterns ...
    path('clients/create/', views.client_create, name='client_create'),
    # path('clients/create/my/', views.my_client_create, name='my_client_create'),  # If exists
]
```

**URLs:**

- **Standard Creation:** `/clients/create/`
- **My Client Creation:** `/clients/create/my/` (if route exists)

**Named URLs:**

- `client_create`: Standard client creation
- `my_client_create`: My client creation (if exists)

**Usage in Templates:**

```django
<a href="{% url 'client_create' %}">Add Client</a>
```

---

## VALIDATION RULES

### Client Name

**Rule:** Required (cannot be empty)

**Validation:**
- ✅ Checked in view: `if name:`
- ✅ HTML5 validation: `required` attribute
- ✅ Max length: 200 characters (database constraint)

**Error Message:**
```
"Client name is required."
```

**Example:**
- ✅ Valid: "John Doe", "ABC Company"
- ❌ Invalid: "" (empty), None

---

### Client Code

**Rule:** Optional, but if provided must be unique

**Validation:**
- ✅ Database constraint: `unique=True`
- ✅ Max length: 50 characters
- ✅ Can be NULL or empty string (converted to None)

**Error Handling:**
- Database-level uniqueness check
- Django raises `IntegrityError` if duplicate

**Example:**
- ✅ Valid: "CLI001", "ABC-123", None, ""
- ❌ Invalid: Duplicate code (if another client has same code)

---

### Referred By

**Rule:** Optional

**Validation:**
- ✅ Max length: 200 characters
- ✅ Can be NULL or empty string (converted to None)

**Example:**
- ✅ Valid: "John Smith", "Marketing Team", None, ""

---

### Company Client Flag

**Rule:** Optional checkbox

**Validation:**
- ✅ Boolean: True if checked, False if unchecked
- ✅ Default: False

**Processing:**
```python
is_company_client = request.POST.get("is_company_client") == "on"
```

**Example:**
- ✅ Checked: `is_company_client = True`
- ✅ Unchecked: `is_company_client = False`

---

### User Assignment

**Rule:** Automatically assigned to logged-in user

**Validation:**
- ✅ `@login_required` decorator ensures user is authenticated
- ✅ `request.user` is always available

**Processing:**
```python
Client.objects.create(
    user=request.user,  # Always set
    # ... other fields ...
)
```

---

## DATA FLOW

### Complete Flow Diagram

```
┌─────────────────┐
│  User Clicks    │
│  "Add Client"   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  GET Request    │
│  /clients/create│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  View:          │
│  client_create  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Render Form    │
│  Template       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  User Fills     │
│  Form & Submit  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  POST Request   │
│  /clients/create│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Extract Form   │
│  Data           │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Validate Name  │
│  (Required)     │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌──────┐  ┌──────────┐
│ Valid│  │ Invalid  │
└───┬──┘  └────┬─────┘
    │          │
    │          ▼
    │    ┌──────────┐
    │    │ Show     │
    │    │ Error    │
    │    └──────────┘
    │
    ▼
┌─────────────────┐
│  Create Client  │
│  Object         │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Save to DB     │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌──────┐  ┌──────────┐
│Success│  │ DB Error │
└───┬──┘  └────┬─────┘
    │          │
    │          ▼
    │    ┌──────────┐
    │    │ Show     │
    │    │ Error    │
    │    └──────────┘
    │
    ▼
┌─────────────────┐
│  Success        │
│  Message        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Redirect to    │
│  Client List    │
└─────────────────┘
```

### Step-by-Step Data Processing

**Step 1: Form Submission**
```
User submits form → POST request → View receives request.POST
```

**Step 2: Data Extraction**
```python
name = request.POST.get("name", "").strip()
code = request.POST.get("code", "").strip()
referred_by = request.POST.get("referred_by", "").strip()
is_company_client = request.POST.get("is_company_client") == "on"
```

**Step 3: Validation**
```python
if name:  # Name is required
    # Proceed
else:
    # Show error
```

**Step 4: Data Normalization**
```python
code = code if code else None  # Convert empty string to None
referred_by = referred_by if referred_by else None
```

**Step 5: Database Insert**
```python
Client.objects.create(
    user=request.user,
    name=name,
    code=code,
    referred_by=referred_by,
    is_company_client=is_company_client,
)
```

**Step 6: Response**
```python
messages.success(request, f"Client '{name}' has been created successfully.")
return redirect(reverse("client_list"))
```

---

## EDGE CASES & SCENARIOS

### Edge Case 1: Empty Name

**Scenario:**
- User submits form with empty name field

**Behavior:**
- HTML5 validation prevents submission (if browser supports)
- View validation: `if name:` fails
- Error message: "Client name is required."
- Form re-rendered with error

**Code:**
```python
if name:
    # Create client
else:
    messages.error(request, "Client name is required.")
    return render(request, "core/clients/create.html")
```

---

### Edge Case 2: Duplicate Code

**Scenario:**
- User tries to create client with code "CLI001"
- Another client already has code "CLI001"

**Behavior:**
- Database raises `IntegrityError`
- Django shows database error message
- Form submission fails

**Prevention:**
- Check for duplicate before creation (optional enhancement)
- Show user-friendly error message

**Current Behavior:**
- Database constraint enforces uniqueness
- Django shows technical error

---

### Edge Case 3: Very Long Name

**Scenario:**
- User enters name longer than 200 characters

**Behavior:**
- HTML5 `maxlength` attribute prevents input (if set)
- Database constraint: `CharField(max_length=200)`
- Database truncates or raises error (depending on DB)

**Prevention:**
- Add HTML5 `maxlength="200"` to input field (recommended)

---

### Edge Case 4: Special Characters in Name

**Scenario:**
- User enters name with special characters: "John's Company & Co."

**Behavior:**
- ✅ Allowed (no restrictions)
- Stored as-is in database
- Displayed correctly in UI

---

### Edge Case 5: Whitespace-Only Name

**Scenario:**
- User enters name with only spaces: "   "

**Behavior:**
- `.strip()` removes whitespace: `name = "   ".strip() → ""`
- Empty string fails validation
- Error: "Client name is required."

**Code:**
```python
name = request.POST.get("name", "").strip()  # Removes whitespace
if name:  # Empty string is falsy
    # Create client
```

---

### Edge Case 6: Code with Special Characters

**Scenario:**
- User enters code: "CLI-001", "CLI_001", "CLI.001"

**Behavior:**
- ✅ Allowed (no restrictions)
- Stored as-is
- Must be unique if provided

---

### Edge Case 7: Empty Code Field

**Scenario:**
- User leaves code field empty

**Behavior:**
- Empty string converted to None: `code if code else None`
- Stored as NULL in database
- No uniqueness check (NULL values don't violate unique constraint)

**Code:**
```python
code = request.POST.get("code", "").strip()
code = code if code else None  # Convert to None
```

---

### Edge Case 8: User Not Logged In

**Scenario:**
- User tries to access `/clients/create/` without login

**Behavior:**
- `@login_required` decorator redirects to login page
- After login, redirects back to creation page

**Protection:**
```python
@login_required
def client_create(request):
    # View code
```

---

### Edge Case 9: Concurrent Creation with Same Code

**Scenario:**
- Two users try to create clients with same code simultaneously

**Behavior:**
- Database-level uniqueness constraint prevents duplicate
- First creation succeeds
- Second creation fails with `IntegrityError`

**Protection:**
- Database constraint: `unique=True` on `code` field

---

### Edge Case 10: Company Client Checkbox Not Checked

**Scenario:**
- User submits form without checking "Company Client"

**Behavior:**
- `request.POST.get("is_company_client")` returns `None`
- `None == "on"` evaluates to `False`
- `is_company_client = False` (default)

**Code:**
```python
is_company_client = request.POST.get("is_company_client") == "on"
# If checkbox not checked: is_company_client = False
```

---

## CODE REFERENCE

### Model: Client

**File:** `core/models.py`

**Lines:** 20-34

**Full Code:**
```python
class Client(TimeStampedModel):
    """
    Client entity - trades on exchange, receives FULL profit, pays FULL loss.
    """
    name = models.CharField(max_length=200)
    code = models.CharField(max_length=50, blank=True, null=True, unique=True)
    referred_by = models.CharField(max_length=200, blank=True, null=True)
    is_company_client = models.BooleanField(default=False)
    user = models.ForeignKey(User, on_delete=models.SET_NULL, null=True, blank=True, related_name='clients')
    
    class Meta:
        ordering = ['name']
    
    def __str__(self):
        return self.name
```

---

### View: client_create

**File:** `core/views.py`

**Lines:** 644-667

**Full Code:**
```python
@login_required
def client_create(request):
    """Create a new client"""
    if request.method == "POST":
        name = request.POST.get("name", "").strip()
        code = request.POST.get("code", "").strip()
        referred_by = request.POST.get("referred_by", "").strip()
        is_company_client = request.POST.get("is_company_client") == "on"
        
        if name:
            Client.objects.create(
                user=request.user,
                name=name,
                code=code if code else None,
                referred_by=referred_by if referred_by else None,
                is_company_client=is_company_client,
            )
            from django.contrib import messages
            messages.success(request, f"Client '{name}' has been created successfully.")
            return redirect(reverse("client_list"))
        else:
            from django.contrib import messages
            messages.error(request, "Client name is required.")
    
    return render(request, "core/clients/create.html")
```

---

### View: my_client_create

**File:** `core/views.py`

**Lines:** 676-697

**Full Code:**
```python
@login_required
def my_client_create(request):
    """Create a my (personal) client"""
    if request.method == "POST":
        from django.shortcuts import redirect
        from django.urls import reverse
        from core.models import Client

        name = request.POST.get("name", "").strip()
        code = request.POST.get("code", "").strip()
        referred_by = request.POST.get("referred_by", "").strip()
        if name:
            Client.objects.create(
                user=request.user,
                name=name,
                code=code if code else None,
                referred_by=referred_by if referred_by else None,
            )
            return redirect("client_list")

    return render(request, "core/clients/create_my.html")
```

---

### Template: create.html

**File:** `core/templates/core/clients/create.html`

**Full Structure:**
```html
{% extends "core/base.html" %}

{% block title %}Add Client · Transaction Hub{% endblock %}
{% block page_title %}Add Client{% endblock %}
{% block page_subtitle %}Create a new client profile{% endblock %}

{% block content %}
<div class="card" style="max-width: 420px;">
    {% if messages %}
        {% for message in messages %}
            <div style="padding: 12px 16px; border-radius: 8px; margin-bottom: 20px; 
                {% if message.tags == 'success' %}background: #d1fae5; color: #065f46; border: 1px solid #10b981;
                {% elif message.tags == 'error' %}background: #fee2e2; color: #991b1b; border: 1px solid #dc2626;
                {% else %}background: #dbeafe; color: #1e40af; border: 1px solid #3b82f6;{% endif %}">
                {{ message }}
            </div>
        {% endfor %}
    {% endif %}
    
    <form method="post">
        {% csrf_token %}
        <div class="form-row">
            <label class="field-label">Client Name *</label>
            <input type="text" name="name" class="field-input" required>
        </div>
        <div class="form-row">
            <label class="field-label">Client Code <span style="color: var(--muted); font-weight: normal;">(Optional)</span></label>
            <input type="text" name="code" class="field-input" placeholder="Leave blank if not needed">
        </div>
        <div class="form-row">
            <label class="field-label">Referred By <span style="color: var(--muted); font-weight: normal;">(Optional)</span></label>
            <input type="text" name="referred_by" class="field-input" placeholder="Name of person who referred this client">
        </div>
        <div class="form-row">
            <label style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
                <input type="checkbox" name="is_company_client" style="width: auto;">
                <span class="field-label" style="margin: 0;">Company Client</span>
            </label>
            <div style="font-size: 13px; color: var(--muted); margin-top: 4px; margin-left: 26px;">
                Check this if this is a company client. Leave unchecked for your personal clients.
            </div>
        </div>
        <div style="display: flex; gap: 12px; margin-top: 20px;">
            <button type="submit" class="btn btn-primary">Save Client</button>
            <a href="{% url 'client_list' %}" class="btn">Cancel</a>
        </div>
    </form>
</div>
{% endblock %}
```

---

### URL Configuration

**File:** `core/urls.py`

**Pattern:**
```python
path('clients/create/', views.client_create, name='client_create'),
```

---

## TESTING GUIDE

### Test Scenario 1: Successful Client Creation

**Steps:**
1. Login as authenticated user
2. Navigate to `/clients/create/`
3. Fill form:
   - Name: "John Doe"
   - Code: "CLI001"
   - Referred By: "Jane Smith"
   - Company Client: Unchecked
4. Click "Save Client"

**Expected:**
- ✅ Client created in database
- ✅ Success message: "Client 'John Doe' has been created successfully."
- ✅ Redirect to client list
- ✅ Client appears in list

**Database Check:**
```python
client = Client.objects.get(name="John Doe")
assert client.name == "John Doe"
assert client.code == "CLI001"
assert client.referred_by == "Jane Smith"
assert client.is_company_client == False
assert client.user == request.user
```

---

### Test Scenario 2: Empty Name Validation

**Steps:**
1. Login as authenticated user
2. Navigate to `/clients/create/`
3. Leave name field empty
4. Fill other fields
5. Click "Save Client"

**Expected:**
- ❌ Client NOT created
- ❌ Error message: "Client name is required."
- ✅ Form re-rendered with error
- ✅ Other fields preserved (if implemented)

---

### Test Scenario 3: Duplicate Code Prevention

**Steps:**
1. Create client with code "CLI001"
2. Try to create another client with code "CLI001"

**Expected:**
- ❌ Second creation fails
- ❌ Database error (IntegrityError)
- ✅ First client remains unchanged

**Database Check:**
```python
clients = Client.objects.filter(code="CLI001")
assert clients.count() == 1  # Only one client with this code
```

---

### Test Scenario 4: Company Client Creation

**Steps:**
1. Login as authenticated user
2. Navigate to `/clients/create/`
3. Fill form:
   - Name: "ABC Company"
   - Code: "COMP001"
   - Company Client: Checked
4. Click "Save Client"

**Expected:**
- ✅ Client created
- ✅ `is_company_client = True`
- ✅ Success message shown

**Database Check:**
```python
client = Client.objects.get(name="ABC Company")
assert client.is_company_client == True
```

---

### Test Scenario 5: Optional Fields Empty

**Steps:**
1. Login as authenticated user
2. Navigate to `/clients/create/`
3. Fill only required field:
   - Name: "Test Client"
   - Code: (empty)
   - Referred By: (empty)
   - Company Client: Unchecked
4. Click "Save Client"

**Expected:**
- ✅ Client created successfully
- ✅ `code = None` in database
- ✅ `referred_by = None` in database
- ✅ `is_company_client = False`

**Database Check:**
```python
client = Client.objects.get(name="Test Client")
assert client.code is None
assert client.referred_by is None
assert client.is_company_client == False
```

---

### Test Scenario 6: Whitespace Handling

**Steps:**
1. Login as authenticated user
2. Navigate to `/clients/create/`
3. Fill form:
   - Name: "   John Doe   " (with spaces)
   - Code: "  CLI001  " (with spaces)
4. Click "Save Client"

**Expected:**
- ✅ Name trimmed: "John Doe"
- ✅ Code trimmed: "CLI001"
- ✅ Client created with trimmed values

**Database Check:**
```python
client = Client.objects.get(code="CLI001")
assert client.name == "John Doe"  # Trimmed
assert client.code == "CLI001"  # Trimmed
```

---

### Test Scenario 7: Unauthenticated Access

**Steps:**
1. Logout (if logged in)
2. Navigate to `/clients/create/`

**Expected:**
- ❌ Access denied
- ✅ Redirect to login page
- ✅ After login, redirect back to creation page

---

### Test Scenario 8: My Client Creation

**Steps:**
1. Login as authenticated user
2. Navigate to `/clients/create/my/` (if route exists)
3. Fill form:
   - Name: "My Client"
   - Code: "MY001"
4. Click "Save My Client"

**Expected:**
- ✅ Client created
- ✅ `is_company_client = False` (always)
- ✅ `user = request.user` (always)
- ✅ Redirect to client list

**Database Check:**
```python
client = Client.objects.get(name="My Client")
assert client.is_company_client == False
assert client.user == request.user
```

---

### Test Scenario 9: Long Name Handling

**Steps:**
1. Login as authenticated user
2. Navigate to `/clients/create/`
3. Fill form:
   - Name: "A" * 200 (exactly 200 characters)
4. Click "Save Client"

**Expected:**
- ✅ Client created successfully
- ✅ Name stored as 200 characters

**Database Check:**
```python
client = Client.objects.get(name="A" * 200)
assert len(client.name) == 200
```

---

### Test Scenario 10: Special Characters in Name

**Steps:**
1. Login as authenticated user
2. Navigate to `/clients/create/`
3. Fill form:
   - Name: "John's Company & Co. (Ltd.)"
4. Click "Save Client"

**Expected:**
- ✅ Client created successfully
- ✅ Name stored with special characters
- ✅ Displayed correctly in UI

**Database Check:**
```python
client = Client.objects.get(name="John's Company & Co. (Ltd.)")
assert client.name == "John's Company & Co. (Ltd.)"
```

---

## SUMMARY

### Key Components

1. **Database Model:** `Client` (TimeStampedModel)
   - Fields: name, code, referred_by, is_company_client, user
   - Constraints: code unique, name required

2. **Views:** `client_create`, `my_client_create`
   - Handle GET (show form) and POST (create client)
   - Validate name, normalize data, create client

3. **Templates:** `create.html`, `create_my.html`
   - Form with fields, validation, messages

4. **URLs:** `/clients/create/`, `/clients/create/my/`
   - Route requests to views

### Validation Rules

- ✅ Name: Required, max 200 characters
- ✅ Code: Optional, unique if provided, max 50 characters
- ✅ Referred By: Optional, max 200 characters
- ✅ Company Client: Optional checkbox, default False
- ✅ User: Automatically assigned (logged-in user)

### Data Flow

1. User navigates to creation page
2. Form displayed
3. User submits form
4. View validates and processes data
5. Client created in database
6. Success message shown
7. Redirect to client list

### Error Handling

- Empty name: Error message shown
- Duplicate code: Database error
- Unauthenticated: Redirect to login
- Validation failures: Form re-rendered with errors

---

**END OF DOCUMENTATION**

