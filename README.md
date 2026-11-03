# Clinic Patient Flow Board (Kanban for Waiting Rooms)

A lightweight, robust, high-performance desktop application for outpatient clinics and waiting rooms built in **Pure Java (JDK 11+) using only Swing/AWT**, with **zero external libraries or Maven/Gradle dependencies**.

---

## 🌟 Highlights & Key Features

- **5-Column Visual Kanban Board**:
  - `Waiting` ➔ `With Doctor` ➔ `Tests` ➔ `Billing` ➔ `Done`
- **Strict Workflow Transition Rules**:
  - Enforces clinic stages and blocks invalid card movement with clear explanations.
  - Admin override allowed for exceptional exits (e.g., patient left early).
- **Intelligent Queue Ordering**:
  - Priority sorting: `Emergency` (rank 3) ➔ `Senior / Child` (rank 2) ➔ `Normal` (rank 1).
  - First-Come, First-Served (FCFS) within equal priority levels.
  - Emergency cards are highlighted with red borders and automatically jump to the top.
  - The head of the waiting line is clearly marked with a green **NEXT** badge.
- **Drag-and-Drop + Context Menu Interaction**:
  - Fluid card drag-and-drop between columns with visual hover indicators.
  - Alternative single-click "→ [Next Stage]" buttons directly on cards.
  - Full right-click context menu (Move to Stage, Edit Details, Notes, Priority, Audit History, Cancel).
- **Dedicated Waiting Room Display (TV/Monitor)**:
  - Separate full-screen / monitor window (`F11` toggle).
  - **100% Patient Privacy compliant**: Shows only Token Number, Room/Doctor, and Stage (no names, phone numbers, or medical reasons).
  - Highlights "NOW SERVING" and "NEXT IN LINE" (top 5 tokens).
  - Updates automatically in real-time via the Observer pattern.
- **Live Timers & Wait Bottleneck Alerts**:
  - Live `javax.swing.Timer` updating every second on the EDT.
  - Tracks both time in current stage (`mm:ss`) and total visit duration.
  - Configurable alert thresholds:
    - Amber highlight for patients waiting > 30 minutes.
    - Red highlight for patients waiting > 60 minutes.
    - Audible chime (`Toolkit.beep()`) when a patient crosses threshold.
- **Daily Analytics & Custom Graphics2D Bar Chart**:
  - Daily metrics: Total Registered, Completed, Active, and Cancelled.
  - Averages: Average wait time, average consultation time, average visit duration.
  - Bottleneck insights: Longest wait of the day and busiest arrival hour.
  - **Pure `Graphics2D` Hourly Bar Chart**: Custom painted without any external chart libraries.
  - Export reports to readable `.txt` or `.csv` files.
- **Role-Based Access Control & Security**:
  - Roles: `Administrator`, `Receptionist`, `Doctor`.
  - Passwords hashed securely using SHA-256 (`MessageDigest`).
  - Pre-seeded users with fast switching for easy testing and demonstration.
- **Crash-Resistant Atomic File Persistence**:
  - Automatically saves on every state change using `java.nio.file`.
  - Atomic write pattern (`.tmp` write ➔ atomic move/replace + `.bak` backup) guarantees zero data corruption on abrupt shutdowns.
  - Plain CSV and Properties files (`data/patients.csv`, `data/history.csv`, `data/users.csv`, `data/settings.properties`).

---

## 🏗️ Project Architecture

```
c:\naya app\
├── build.bat                  # One-click Windows build script
├── run.bat                    # One-click launch script
├── test.bat                   # Automated test runner
├── README.md                  # Project documentation
├── bin\                       # Compiled Java bytecode (.class files)
├── data\                      # Local CSV and properties files
│   ├── patients.csv
│   ├── history.csv
│   ├── users.csv
│   └── settings.properties
└── src\
    └── clinicflow\
        ├── Main.java          # Application entry point
        ├── model\
        │   ├── Patient.java       # Patient entity with history
        │   ├── Priority.java      # Normal, Senior/Child, Emergency
        │   ├── Role.java          # Admin, Receptionist, Doctor permissions
        │   ├── Settings.java      # Configurable thresholds and preferences
        │   ├── Stage.java         # 5 Kanban stages and transition rules
        │   ├── StageRecord.java   # Audit timestamps (enteredAt, exitedAt)
        │   └── User.java          # Staff user account model
        ├── service\
        │   ├── AuthService.java       # SHA-256 hashing and session management
        │   ├── PatientListener.java   # Observer event interface
        │   ├── PatientService.java    # Queue prioritization, CRUD, moves
        │   ├── ReportService.java     # Daily analytics, averages, exports
        │   └── TokenGenerator.java    # Sequential daily tokens (T-001, ...)
        ├── storage\
        │   └── DataStore.java         # Atomic CSV & properties persistence
        ├── ui\
        │   ├── BoardPanel.java         # 5 columns layout container
        │   ├── CardPanel.java          # Patient card with timer & DnD
        │   ├── ColumnPanel.java        # Kanban stage column & drop target
        │   ├── DisplayFrame.java       # TV waiting room screen (F11)
        │   ├── HourlyBarChartPanel.java# Pure Graphics2D custom chart
        │   ├── LoginDialog.java        # Staff login & quick switcher
        │   ├── MainFrame.java          # Master window, toolbar & timers
        │   ├── PatientDialog.java      # Patient registration & edit modal
        │   ├── SettingsDialog.java     # System configuration modal
        │   ├── SummaryDialog.java      # Daily summary & chart modal
        │   └── Theme.java              # Color palette and typography
        ├── util\
        │   ├── TimeUtil.java           # Duration & ISO formatters
        │   └── Validator.java          # Input validation checks
        └── test\
            └── ClinicFlowTestSuite.java# Comprehensive pure JDK test suite
```

---

## 🚀 Quick Start & How to Run

### Prerequisites
- JDK 11 or higher installed (tested and verified on JDK 26).
- Zero external libraries or build tools needed.

### 1. Compile the Code
Run the provided batch file or execute `javac`:
```cmd
build.bat
```
*Or manually:*
```cmd
javac -d bin (Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object { $_.FullName })
```

### 2. Launch the Application
Run the launcher script or execute `java`:
```cmd
run.bat
```
*Or manually:*
```cmd
java -cp bin clinicflow.Main
```

### 3. Run the Automated Test Suite
```cmd
test.bat
```
*Or manually:*
```cmd
java -cp bin clinicflow.test.ClinicFlowTestSuite
```

---

## 👥 Default User Accounts

The application automatically seeds default staff accounts in `data/users.csv`:

| Username | Password | Role | Description |
|---|---|---|---|
| `admin` | `admin123` | **Administrator** | Full access to all operations, overrides, settings, and reports. |
| `reception` | `rec123` | **Receptionist** | Can register patients, move cards, manage queue, view display. |
| `dr_sharma` | `doc123` | **Doctor** | Dr. A. Sharma (General Medicine) - call patients, order tests/billing. |
| `dr_patel` | `doc123` | **Doctor** | Dr. P. Patel (Pediatrics) - call patients, order tests/billing. |

*Tip: Use the "Switch User" button in the top-right header to switch roles instantly.*

---

## 📋 Workflow Stages & Allowed Transitions

```
[ WAITING ] ───────► [ WITH DOCTOR ] ───────► [ TESTS ]
                          │                       │
                          │                       ▼
                          ├─────────────────► [ BILLING ]
                          │                       │
                          ▼                       ▼
                       [ DONE ] ◄─────────────────┘
```

- **Waiting ➔ With Doctor**: Initiated via "Call Next Patient", card action, or drag-and-drop.
- **With Doctor ➔ Tests, Billing, or Done**: Depending on consultation outcome.
- **Tests ➔ With Doctor or Billing**: Returns to doctor for result review or proceeds to billing.
- **Billing ➔ Done**: Visit completed after payment.
- **Admin Override**: Administrators can move any active card directly to `Done` if a patient leaves early.
- **Invalid moves**: Blocked with an explanatory dialog message.

---

## 📊 Daily Summary & Report Export

Click **"📊 Daily Summary"** in the top navigation bar to open the analytics dashboard:
- Visual volume counters and average wait time calculations.
- Bottleneck insights (longest wait of the day, peak arrival hour).
- **Custom Graphics2D Bar Chart**: Shows arrivals per hour from 8:00 AM to 8:00 PM with peak highlights and average baseline.
- **Export to Text (`.txt`)**: Generates an end-of-day audit summary report.
- **Export to CSV (`.csv`)**: Exports full patient visit logs with granular timestamps for spreadsheet analysis.

---

## 📺 Waiting Room TV Display

Click **"📺 Display Screen"** in the top navigation bar:
- Designed for secondary monitors or waiting room TVs.
- Shows live token numbers in high-contrast typography.
- Press **`F11`** to toggle full-screen mode.
- Does not expose patient names or private health information.
