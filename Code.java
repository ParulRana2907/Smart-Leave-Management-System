import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// ========================
// SMART LEAVE SYSTEM
// Single-file with CSV + TXT Export (Option A)
// ========================
public class SmartLeaveSystem {

    // ---------- Utilities ----------
    static class Colors {
        public static final String RESET = "\u001B[0m";
        public static final String RED = "\u001B[31m";
        public static final String GREEN = "\u001B[32m";
        public static final String YELLOW = "\u001B[33m";
        public static final String CYAN = "\u001B[36m";
        public static final String PURPLE = "\u001B[35m";
        public static String color(String msg, String color) { return color + msg + RESET; }
    }
    static void printlnError(String msg) { System.out.println(Colors.color("🚫 " + msg, Colors.RED)); }
    static void printlnSuccess(String msg) { System.out.println(Colors.color("✅ " + msg, Colors.GREEN)); }
    static void printlnInfo(String msg) { System.out.println(Colors.color("\u2139 " + msg, Colors.CYAN)); }

    static final int DEFAULT_LEAVES_PER_YEAR = 30;
    static Scanner sc = new Scanner(System.in);
    static Random rnd = new Random();

    // Instance data
    private List<User> users = new ArrayList<>();
    private List<LeaveRequest> requests = new ArrayList<>();
    private List<HRFeedback> feedbacks = new ArrayList<>();

    // ---------- Validators ----------
    static class InputValidator {
        public static boolean isValidEmail(String email) {
            return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        }
        public static boolean isValidDate(String date) {
            try { LocalDate.parse(date); return true; }
            catch (Exception e) { return false; }
        }
        public static boolean isInt(String s) {
            try { Integer.parseInt(s); return true; }
            catch (Exception e) { return false; }
        }
    }

    // ---------- Table Formatter ----------
    static class TableFormatter {
        public static String fit(String s, int len) {
            if (s == null) s = "";
            if (s.length() > len) return s.substring(0, len - 2) + "…";
            return String.format("%-" + len + "s", s);
        }
        public static void printTableHeader(String[] headers, int[] widths) {
            String border = "+";
            for (int w : widths) border += "-".repeat(w + 2) + "+";
            System.out.println(border);
            System.out.print("|");
            for (int i = 0; i < headers.length; i++)
                System.out.print(" " + fit(headers[i], widths[i]) + " |");
            System.out.println();
            System.out.println(border);
        }
        public static void printRow(String[] cols, int[] widths) {
            System.out.print("|");
            for (int i = 0; i < widths.length; i++)
                System.out.print(" " + fit(cols[i], widths[i]) + " |");
            System.out.println();
        }
        public static void printTableFooter(int[] widths) {
            String border = "+";
            for (int w : widths) border += "-".repeat(w + 2) + "+";
            System.out.println(border);
        }
    }

    // ---------- Models & OOP ----------
    abstract static class User {
        private int empId, leaveBalance, badges;
        private final int totalLeavesAllowed = DEFAULT_LEAVES_PER_YEAR;
        private String name, email, password, lastLogin;

        public User(int empId, String name, String email, String password, int leaveBalance) {
            this.empId = empId;
            this.name = name;
            this.email = email;
            this.password = password;
            this.leaveBalance = leaveBalance;
            this.badges = 0;
            this.lastLogin = "Never";
        }
        public int getEmpId() { return empId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPassword() { return password; }
        public int getLeaveBalance() { return leaveBalance; }
        public void setLeaveBalance(int lb) { this.leaveBalance = lb; }
        public int getBadges() { return badges; }
        public void setBadges(int b) { this.badges = b; }
        public int getTotalLeavesAllowed() { return totalLeavesAllowed; }
        public String getLastLogin() { return lastLogin; }
        public void setLastLogin(String ll) { this.lastLogin = ll; }
        public abstract void viewDashboard(SmartLeaveSystem sys);
    }

    static class Employee extends User {
        public Employee(int empId, String name, String email, String password, int leaveBalance) {
            super(empId, name, email, password, leaveBalance);
        }
        @Override
        public void viewDashboard(SmartLeaveSystem sys) { sys.employeeMenu(this); }
    }

    static class Manager extends User {
        public Manager(int empId, String name, String email, String password, int leaveBalance) {
            super(empId, name, email, password, leaveBalance);
        }
        @Override
        public void viewDashboard(SmartLeaveSystem sys) { sys.managerMenu(this); }
    }

    static class Admin extends User {
        public Admin(int empId, String name, String email, String password, int leaveBalance) {
            super(empId, name, email, password, leaveBalance);
        }
        @Override
        public void viewDashboard(SmartLeaveSystem sys) { sys.adminMenu(this); }
    }

    static class LeaveRequest {
        private static int counter = 1000;
        private int reqId, empId, requestedDays;
        private String start, end, type, status, comments;

        public LeaveRequest(int eid, String start, String end, String type, String comm, int days) {
            this.reqId = counter++;
            this.empId = eid;
            this.start = start;
            this.end = end;
            this.type = type;
            this.status = "PENDING";
            this.comments = comm;
            this.requestedDays = days;
        }
        public int getReqId() { return reqId; }
        public int getEmpId() { return empId; }
        public String getStart() { return start; }
        public String getEnd() { return end; }
        public String getType() { return type; }
        public String getStatus() { return status; }
        public void setStatus(String s) { this.status = s; }
        public String getComments() { return comments; }
        public int getRequestedDays() { return requestedDays; }
    }

    static class HRFeedback {
        private String empName, message;
        public HRFeedback(String empName, String message) {
            this.empName = empName; this.message = message;
        }
        public String getEmpName() { return empName; }
        public String getMessage() { return message; }
    }

    // ---------- Extra Features ----------
    static class BlockchainSimulator {
        public static String hashLeave(LeaveRequest req) {
            String content = req.getReqId() + ":" + req.getEmpId() + ":" + req.getStart() + ":" + req.getEnd() + ":" + req.getType() + ":" + req.getStatus();
            return Integer.toHexString(content.hashCode());
        }
        public static void printLeaveChain(List<LeaveRequest> requests) {
            System.out.println(Colors.color("🛡 Blockchain Leave Chain:", Colors.PURPLE));
            for (LeaveRequest req : requests)
                System.out.println("Block: ReqID " + req.getReqId() + " | Hash: " + hashLeave(req) + " | Status: " + req.getStatus());
        }
        public static void verifyIntegrity(List<LeaveRequest> requests) {
            System.out.println("Verifying hashes for all requests…");
            for (LeaveRequest req : requests) {
                String hash = hashLeave(req);
                System.out.println("Request " + req.getReqId() + " hash: " + hash + " [OK]");
            }
            printlnSuccess("All hashes verified.");
        }
        public static void auditTrail(List<LeaveRequest> requests) {
            System.out.println("Audit Trail (Hashes):");
            for (LeaveRequest req : requests)
                System.out.println("ReqID:" + req.getReqId() + " | Status:" + req.getStatus() + " | Hash:" + hashLeave(req));
        }
    }

    static class LeavePatternPredictor {
        public static void predict(User u, List<LeaveRequest> requests) {
            int total = 0, sick = 0, wfh = 0, vac = 0;
            for (LeaveRequest r : requests)
                if (r.getEmpId() == u.getEmpId()) {
                    total++;
                    String t = r.getType().toLowerCase();
                    if (t.contains("sick")) sick++;
                    if (t.contains("wfh")) wfh++;
                    if (t.contains("vac")) vac++;
                }
            System.out.println("🔮 Past leaves: " + total + " | Sick: " + sick + " | WFH: " + wfh + " | Vacation: " + vac);
            System.out.println("Predicted: Next leave could be " + (wfh > sick && wfh > vac ? "WFH" : sick > vac ? "Sick" : "Vacation"));
        }
    }

    static class AIStressDetector {
        public static void analyze(User u) {
            int leaveTaken = u.getTotalLeavesAllowed() - u.getLeaveBalance();
            String suggestion = leaveTaken > 20 ? "⚠ High Leaves: You might be stressed. Take wellness leave or consult HR."
                    : leaveTaken < 5 ? "✅ Good leave pattern. Keep balancing work and rest."
                    : "🟡 Moderate leave. Prioritize self-care.";
            System.out.println("AI Stress Analysis:");
            System.out.println("Leaves taken: " + leaveTaken);
            System.out.println("Suggestion: " + suggestion);
        }
    }

    static class QRGenerator {
        public static void printQR(String data) {
            System.out.println("ASCII QR for [" + data + "]:");
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 18; j++) {
                    if (rnd.nextBoolean()) System.out.print("█");
                    else System.out.print(" ");
                }
                System.out.println();
            }
            System.out.println("Scan simulation: Console QR not for phone cameras. Real QR would link profile or verification page.");
        }
    }

    // ---------- Demo Data ----------
    void loadDemoData() {
        users.add(new Employee(101, "Shubhangi Tyagi", "shubhangi@email.com", "pass123", 24));
        users.add(new Manager(201, "Parul Rana", "parul@email.com", "manager1", 30)); // Updated this line
        users.add(new Admin(301, "Dr Swati Gupta", "admin@email.com", "admin2050", 50));
        LeaveRequest req = new LeaveRequest(101, "2025-11-10", "2025-11-11", "WFH", "Remote work", 2);
        req.setStatus("APPROVED");
        requests.add(req);
    }

    // ---------- Prompts ----------
    String promptValidDate(String prompt) {
        while (true) {
            System.out.print(prompt + " [YYYY-MM-DD]: ");
            String in = sc.nextLine().trim();
            if (InputValidator.isValidDate(in)) return in;
            printlnError("Invalid date! Please re-enter.");
        }
    }
    String promptValidEmail(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            String in = sc.nextLine().trim();
            if (InputValidator.isValidEmail(in)) return in;
            printlnError("Invalid email! Please re-enter.");
        }
    }
    int promptInt(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            String in = sc.nextLine().trim();
            if (InputValidator.isInt(in)) return Integer.parseInt(in);
            printlnError("Not a valid number! Try again.");
        }
    }

    // ---------- File helpers ----------
    private static String timestampForFile() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
    private static void saveToFile(String fileName, String data) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.print(data);
            printlnSuccess("Saved: " + fileName);
        } catch (IOException e) {
            printlnError("Failed to save file: " + e.getMessage());
        }
    }
    private static String sanitize(String s) {
        if (s == null) return "";
        return s.replaceAll(",", " "); // avoid CSV breaking
    }

    // ---------- Export Generators ----------
    private String generateLeaveCSVString(List<LeaveRequest> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("ReqID,EmpID,Start,End,Days,Type,Status,Comments\n");
        for (LeaveRequest r : list) {
            sb.append(r.getReqId()).append(",")
              .append(r.getEmpId()).append(",")
              .append(r.getStart()).append(",")
              .append(r.getEnd()).append(",")
              .append(r.getRequestedDays()).append(",")
              .append(sanitize(r.getType())).append(",")
              .append(sanitize(r.getStatus())).append(",")
              .append(sanitize(r.getComments())).append("\n");
        }
        return sb.toString();
    }
    private String generateLeaveTXTString(List<LeaveRequest> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Leave Requests Report =====\n\n");
        for (LeaveRequest r : list) {
            sb.append("Request ID: ").append(r.getReqId()).append("\n");
            User u = getUserById(r.getEmpId());
            String name = (u==null) ? "Unknown" : u.getName();
            sb.append("Employee  : ").append(name).append(" (").append(r.getEmpId()).append(")\n");
            sb.append("Type      : ").append(r.getType()).append("\n");
            sb.append("From      : ").append(r.getStart()).append("\n");
            sb.append("To        : ").append(r.getEnd()).append("\n");
            sb.append("Days      : ").append(r.getRequestedDays()).append("\n");
            sb.append("Reason    : ").append(r.getComments()).append("\n");
            sb.append("Status    : ").append(r.getStatus()).append("\n");
            sb.append("----------------------------------------\n");
        }
        return sb.toString();
    }

    private String generateTeamStatsCSVString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EmpID,Name,LeavesUsed,LeaveBalance\n");
        for (User u : users) if (u instanceof Employee) {
            sb.append(u.getEmpId()).append(",")
              .append(sanitize(u.getName())).append(",")
              .append(u.getTotalLeavesAllowed() - u.getLeaveBalance()).append(",")
              .append(u.getLeaveBalance()).append("\n");
        }
        return sb.toString();
    }
    private String generateTeamStatsTXTString() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Team Leave Summary =====\n\n");
        for (User u : users) if (u instanceof Employee) {
            sb.append("EmpID : ").append(u.getEmpId()).append("\n");
            sb.append("Name  : ").append(u.getName()).append("\n");
            sb.append("Leaves Used : ").append(u.getTotalLeavesAllowed() - u.getLeaveBalance()).append("\n");
            sb.append("Leave Balance: ").append(u.getLeaveBalance()).append("\n");
            sb.append("--------------------------------\n");
        }
        return sb.toString();
    }

    private String generateEmployeeCSVString(Employee emp) {
        StringBuilder sb = new StringBuilder();
        sb.append("EmpID,Name,Email,TotalAllowed,LeaveBalance,Badges,LastLogin\n");
        sb.append(emp.getEmpId()).append(",")
          .append(sanitize(emp.getName())).append(",")
          .append(sanitize(emp.getEmail())).append(",")
          .append(emp.getTotalLeavesAllowed()).append(",")
          .append(emp.getLeaveBalance()).append(",")
          .append(emp.getBadges()).append(",")
          .append(sanitize(emp.getLastLogin())).append("\n");
        return sb.toString();
    }
    private String generateEmployeeTXTString(Employee emp) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Employee Profile =====\n");
        sb.append("EmpID: ").append(emp.getEmpId()).append("\n");
        sb.append("Name: ").append(emp.getName()).append("\n");
        sb.append("Email: ").append(emp.getEmail()).append("\n");
        sb.append("Total Allowed: ").append(emp.getTotalLeavesAllowed()).append("\n");
        sb.append("Leave Balance: ").append(emp.getLeaveBalance()).append("\n");
        sb.append("Badges: ").append(emp.getBadges()).append("\n");
        sb.append("Last Login: ").append(emp.getLastLogin()).append("\n");
        sb.append("-----------------------------\n");
        // append their specific requests
        sb.append("\nRequests:\n");
        for (LeaveRequest r : requests) if (r.getEmpId() == emp.getEmpId()) {
            sb.append("Req#").append(r.getReqId()).append(" | ").append(r.getType()).append(" | ")
              .append(r.getStart()).append(" -> ").append(r.getEnd()).append(" | Days: ").append(r.getRequestedDays())
              .append(" | Status: ").append(r.getStatus()).append("\n");
        }
        return sb.toString();
    }

    private String generateFeedbackCSVString() {
        StringBuilder sb = new StringBuilder();
        sb.append("From,Message\n");
        for (HRFeedback fb : feedbacks) {
            sb.append(sanitize(fb.getEmpName())).append(",")
              .append(sanitize(fb.getMessage())).append("\n");
        }
        return sb.toString();
    }
    private String generateFeedbackTXTString() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== HR Feedback =====\n\n");
        for (HRFeedback fb : feedbacks) {
            sb.append("From: ").append(fb.getEmpName()).append("\n");
            sb.append("Message: ").append(fb.getMessage()).append("\n");
            sb.append("--------------------------------\n");
        }
        return sb.toString();
    }

    private String generateBlockchainCSVString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ReqID,EmpID,Status,Hash\n");
        for (LeaveRequest r : requests) {
            sb.append(r.getReqId()).append(",")
              .append(r.getEmpId()).append(",")
              .append(sanitize(r.getStatus())).append(",")
              .append(BlockchainSimulator.hashLeave(r)).append("\n");
        }
        return sb.toString();
    }
    private String generateBlockchainTXTString() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Blockchain Audit Trail =====\n\n");
        for (LeaveRequest r : requests) {
            sb.append("ReqID: ").append(r.getReqId()).append(" | EmpID: ").append(r.getEmpId()).append("\n");
            sb.append("Status: ").append(r.getStatus()).append("\n");
            sb.append("Hash: ").append(BlockchainSimulator.hashLeave(r)).append("\n");
            sb.append("--------------------------------\n");
        }
        return sb.toString();
    }

    // ---------- Main ----------
    public static void main(String[] args) {
        SmartLeaveSystem sys = new SmartLeaveSystem();
        System.out.println(Colors.color("\n════════ SMART LEAVE MANAGEMENT SYSTEM ════════\n", Colors.PURPLE));
        sys.loadDemoData();
        while (true) {
            System.out.println(Colors.color("\nMain Menu", Colors.YELLOW));
            System.out.println("1. Employee Login");
            System.out.println("2. Manager Login");
            System.out.println("3. Admin Login");
            System.out.println("4. Blockchain Features");
            System.out.println("5. Exit");
            System.out.print("Choose: ");
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1":
                    sys.roleLogin(Employee.class); break;
                case "2":
                    sys.roleLogin(Manager.class); break;
                case "3":
                    sys.roleLogin(Admin.class); break;
                case "4":
                    sys.blockchainFeatureMenu(); break;
                case "5":
                    System.out.println("Goodbye!"); return;
                default:
                    printlnError("Try again!");
            }
        }
    }

    // ---------- Login ----------
    void roleLogin(Class<? extends User> clazz) {
        User u = null;
        String email = promptValidEmail("Email");
        System.out.print("Password: "); String pw = sc.nextLine().trim();
        for (User usr : users) {
            if (usr.getEmail().equalsIgnoreCase(email)
                    && usr.getPassword().equals(pw)
                    && clazz.isInstance(usr)) {
                u = usr;
                u.setLastLogin(LocalDate.now() + " " + LocalTime.now().withNano(0));
                printlnSuccess("Logged in as " + u.getName() + " (" + clazz.getSimpleName() + ")");
                break;
            }
        }
        if (u != null)
            u.viewDashboard(this);
        else
            printlnError("No such " + clazz.getSimpleName() + " or wrong credentials.");
    }

    // ========== EMPLOYEE MENU ==========
    void employeeMenu(Employee emp) {
        while (true) {
            printlnInfo("Employee Dashboard (" + emp.getName() + ")");
            System.out.println("Leaves: " + emp.getLeaveBalance() + "/" + emp.getTotalLeavesAllowed() + " | Badges: " + emp.getBadges());
            System.out.println("1. Apply for Leave");
            System.out.println("2. Cancel/Edit Pending Leave");
            System.out.println("3. View Leave History");
            System.out.println("4. View/Export My Data");
            System.out.println("5. Stress Detector");
            System.out.println("6. Leave Pattern Prediction");
            System.out.println("7. Submit HR feedback");
            System.out.println("8. Show QR code (simulation)");
            System.out.println("9. Logout");
            System.out.print("Choose: ");
            String c = sc.nextLine().trim();
            switch (c) {
                case "1": applyLeave(emp); break;
                case "2": cancelEditPendingLeave(emp); break;
                case "3": empHistoryTable(emp); break;
                case "4": exportEmpData(emp); break;
                case "5": AIStressDetector.analyze(emp); break;
                case "6": LeavePatternPredictor.predict(emp, requests); break;
                case "7": feedback(emp); break;
                case "8": QRGenerator.printQR(emp.getName() + "#" + emp.getEmpId()); break;
                case "9": return;
                default: printlnError("Invalid."); break;
            }
        }
    }

    void exportEmpData(Employee u) {
        System.out.println("--- My Data Export ---");
        System.out.println("1. Export CSV");
        System.out.println("2. Export TXT");
        System.out.println("3. Show on screen");
        System.out.print("Choose: ");
        String ch = sc.nextLine().trim();
        if (ch.equals("1")) {
            String csv = generateEmployeeCSVString(u);
            String fname = "employee_" + u.getEmpId() + "_" + timestampForFile() + ".csv";
            saveToFile(fname, csv);
        } else if (ch.equals("2")) {
            String txt = generateEmployeeTXTString(u);
            String fname = "employee_" + u.getEmpId() + "_" + timestampForFile() + ".txt";
            saveToFile(fname, txt);
        } else {
            System.out.println("Name: " + u.getName());
            System.out.println("Leaves Used: " + (u.getTotalLeavesAllowed() - u.getLeaveBalance()));
            System.out.println("Leave Balance: " + u.getLeaveBalance());
            System.out.println("My Requests:");
            empHistoryTable(u);
        }
    }

    void applyLeave(Employee emp) {
        String startDate = promptValidDate("Start Date");
        String endDate = promptValidDate("End Date");
        LocalDate s = LocalDate.parse(startDate);
        LocalDate e = LocalDate.parse(endDate);
        int requestedDays = (int) (e.toEpochDay() - s.toEpochDay()) + 1;
        if (requestedDays < 1) { printlnError("End before Start!"); return; }
        System.out.print("Type (Sick/Casual/WFH/Vacation/Others): ");
        String type = sc.nextLine().trim();
        System.out.print("Reason: ");
        String reason = sc.nextLine();
        if (emp.getLeaveBalance() < requestedDays) { printlnError("Leave balance too low!"); return; }
        requests.add(new LeaveRequest(emp.getEmpId(), startDate, endDate, type, reason, requestedDays));
        emp.setLeaveBalance(emp.getLeaveBalance() - requestedDays);
        printlnSuccess("Leave submitted! Remaining: " + emp.getLeaveBalance());
    }

    void cancelEditPendingLeave(Employee emp) {
        List<LeaveRequest> pending = new ArrayList<>();
        for (LeaveRequest r : requests)
            if (r.getEmpId() == emp.getEmpId() && r.getStatus().equals("PENDING")) pending.add(r);
        if (pending.isEmpty()) { printlnInfo("No pending requests."); return; }
        System.out.println("Your pending requests:");
        for (LeaveRequest r : pending)
            System.out.println("ReqID:" + r.getReqId() + " " + r.getType() + " " + r.getStart() + "-" + r.getEnd() + " [" + r.getStatus() + "]");
        int rid = promptInt("Enter ReqID to Cancel/Edit");
        for (LeaveRequest r : pending) {
            if (r.getReqId() == rid) {
                System.out.print("Cancel(C) or Edit(E)? ");
                String opt = sc.nextLine().trim().toUpperCase();
                if (opt.equals("C")) {
                    emp.setLeaveBalance(emp.getLeaveBalance() + r.getRequestedDays());
                    requests.remove(r);
                    printlnSuccess("Cancelled & leave restored.");
                } else if (opt.equals("E")) {
                    applyLeave(emp);
                    requests.remove(r);
                    printlnSuccess("Edited (old deleted, new added).");
                }
                return;
            }
        }
        printlnInfo("Not found.");
    }

    void empHistoryTable(Employee emp) {
        String[] headers = {"ReqID", "Type", "Start", "End", "Days", "Status", "Comments"};
        int[] widths = {6, 8, 10, 10, 4, 8, 28};
        TableFormatter.printTableHeader(headers, widths);
        boolean any = false;
        for (LeaveRequest r : requests) {
            if (r.getEmpId() == emp.getEmpId()) {
                TableFormatter.printRow(
                        new String[]{"" + r.getReqId(), r.getType(), r.getStart(), r.getEnd(), "" + r.getRequestedDays(), r.getStatus(), r.getComments()},
                        widths);
                any = true;
            }
        }
        if (!any) TableFormatter.printRow(new String[]{"None", "", "", "", "", "", ""}, widths);
        TableFormatter.printTableFooter(widths);
    }
    void feedback(Employee emp) {
        System.out.print("Please enter feedback for HR: ");
        String fb = sc.nextLine();
        feedbacks.add(new HRFeedback(emp.getName(), fb));
        printlnSuccess("Feedback submitted. Thank you!");
        emp.setBadges(emp.getBadges() + 1);
    }

    // ========== MANAGER MENU ==========
    void managerMenu(Manager u) {
        while (true) {
            printlnInfo("Manager Dashboard (" + u.getName() + ")");
            System.out.println("Team Leaves Used: " + getTeamLeavesUsed() + " | Badges: " + u.getBadges());
            System.out.println("1. View All Leave Requests");
            System.out.println("2. Approve/Reject Leave");
            System.out.println("3. View Team Leave Summary");
            System.out.println("4. Download Leave Request Data (CSV/TXT)");
            System.out.println("5. Download Team Statistics (CSV/TXT)");
            System.out.println("6. Team Analytics Dashboard");
            System.out.println("7. Logout");
            System.out.print("Choose: ");
            String c = sc.nextLine().trim();
            switch (c) {
                case "1": viewAllTable(); break;
                case "2": approveReject(); break;
                case "3": viewTeamSummary(); break;
                case "4": downloadLeaveData(); break;
                case "5": downloadTeamStats(); break;
                case "6": analyticsDashboard(); break;
                case "7": return;
                default: printlnError("Invalid."); break;
            }
        }
    }
    int getTeamLeavesUsed() {
        int sum = 0;
        for (User u : users)
            if (u instanceof Employee) sum += u.getTotalLeavesAllowed() - u.getLeaveBalance();
        return sum;
    }
    void viewAllTable() {
        String[] headers = {"ReqID", "EmpID", "Start", "End", "Days", "Type", "Status", "Comments"};
        int[] widths = {6, 6, 10, 10, 4, 8, 8, 20};
        TableFormatter.printTableHeader(headers, widths);
        for (LeaveRequest r : requests) {
            TableFormatter.printRow(
                    new String[]{
                            "" + r.getReqId(), "" + r.getEmpId(), r.getStart(), r.getEnd(), "" + r.getRequestedDays(), r.getType(), r.getStatus(), r.getComments()
                    }, widths);
        }
        TableFormatter.printTableFooter(widths);
    }
    void approveReject() {
        viewAllTable();
        int rid = promptInt("Enter RequestID to Approve/Reject");
        for (LeaveRequest r : requests)
            if (r.getReqId() == rid && r.getStatus().equals("PENDING")) {
                System.out.print("Approve (A) or Reject (R)? ");
                String ch = sc.nextLine().toUpperCase();
                if (ch.equals("A")) {
                    r.setStatus("APPROVED");
                    printlnSuccess("Leave approved.");
                    return;
                } else if (ch.equals("R")) {
                    r.setStatus("REJECTED");
                    User u = getUserById(r.getEmpId());
                    if (u != null) u.setLeaveBalance(u.getLeaveBalance() + r.getRequestedDays());
                    printlnSuccess("Rejected, leave restored.");
                    return;
                }
            }
        printlnInfo("No such pending request.");
    }

    void viewTeamSummary() {
        String[] headers = {"EmpID", "Name", "Leaves Used", "Leave Balance"};
        int[] widths = {6, 20, 12, 12};
        TableFormatter.printTableHeader(headers, widths);
        for (User u : users)
            if (u instanceof Employee)
                TableFormatter.printRow(new String[]{
                        "" + u.getEmpId(), u.getName(), "" + (u.getTotalLeavesAllowed() - u.getLeaveBalance()), "" + u.getLeaveBalance()
                }, widths);
        TableFormatter.printTableFooter(widths);
    }

    // Manager download: actual file writing for Leave Requests
    void downloadLeaveData() {
        System.out.println("Download Leave Request Data:");
        System.out.println("1. Export CSV");
        System.out.println("2. Export TXT");
        System.out.println("3. Show on screen");
        System.out.print("Choose: ");
        String ch = sc.nextLine().trim();
        if (ch.equals("1")) {
            String csv = generateLeaveCSVString(requests);
            String fname = "leave_requests_" + timestampForFile() + ".csv";
            saveToFile(fname, csv);
        } else if (ch.equals("2")) {
            String txt = generateLeaveTXTString(requests);
            String fname = "leave_requests_" + timestampForFile() + ".txt";
            saveToFile(fname, txt);
        } else viewAllTable();
    }

    // Manager download: Team stats
    void downloadTeamStats() {
        System.out.println("Download Team Stats:");
        System.out.println("1. Export CSV");
        System.out.println("2. Export TXT");
        System.out.println("3. Show on screen");
        System.out.print("Choose: ");
        String ch = sc.nextLine().trim();
        if (ch.equals("1")) {
            String csv = generateTeamStatsCSVString();
            String fname = "team_stats_" + timestampForFile() + ".csv";
            saveToFile(fname, csv);
        } else if (ch.equals("2")) {
            String txt = generateTeamStatsTXTString();
            String fname = "team_stats_" + timestampForFile() + ".txt";
            saveToFile(fname, txt);
        } else viewTeamSummary();
    }

    void analyticsDashboard() {
        System.out.println("--- Team Analytics ---");
        int total = 0, maxDays = 0;
        User topUser = null;
        for (User u : users) {
            int taken = u.getTotalLeavesAllowed() - u.getLeaveBalance();
            total += taken;
            if (taken > maxDays) {
                maxDays = taken;
                topUser = u;
            }
        }
        System.out.println("Total leaves by team: " + total);
        if (topUser != null)
            System.out.println("Top absentee: " + topUser.getName() + " (" + maxDays + " leaves)");
        int pending = 0;
        for (LeaveRequest r : requests) if (r.getStatus().equalsIgnoreCase("PENDING")) pending++;
        System.out.println("Current pending leave requests: " + pending);
    }

    // ========== ADMIN MENU ==========
    void adminMenu(Admin admin) {
        while (true) {
            printlnInfo("Admin Dashboard (" + admin.getName() + ")");
            System.out.println("Total Employees: " + countRole(Employee.class) + ", Total Requests: " + requests.size());
            System.out.println("1. Organization Stats");
            System.out.println("2. Month/Year Attendance Summary");
            System.out.println("3. View HR Feedback");
            System.out.println("4. Award Board");
            System.out.println("5. Announce Policy Update");
            System.out.println("6. Export HR Feedback");
            System.out.println("7. Blockchain/Audit Features");
            System.out.println("8. Logout");
            System.out.print("Choose: ");
            String c = sc.nextLine().trim();
            switch (c) {
                case "1": orgStatsTable(); break;
                case "2": attendanceSummary(); break;
                case "3": hrFeedbackTable(); break;
                case "4": awardBoard(); break;
                case "5": policyUpdate(); break;
                case "6": exportHRFeedback(); break;
                case "7": blockchainFeatureMenu(); break;
                case "8": return;
                default: printlnError("Invalid."); break;
            }
        }
    }

    int countRole(Class<?> clazz) {
        int cnt = 0;
        for (User u : users) if (clazz.isInstance(u)) cnt++;
        return cnt;
    }
    void orgStatsTable() {
        System.out.println("--- Org-wide Leave Stats ---");
        System.out.println("Employees: " + countRole(Employee.class));
        int totalLeaves = 0;
        for (User u : users)
            if (u instanceof Employee) totalLeaves += (u.getTotalLeavesAllowed() - u.getLeaveBalance());
        System.out.println("Leaves taken this year: " + totalLeaves);
        int reqs = requests.size();
        System.out.println("Total requests this year: " + reqs);
        int approved = 0, rejected = 0, pending = 0;
        for (LeaveRequest r : requests) {
            if (r.getStatus().equals("APPROVED")) approved++;
            else if (r.getStatus().equals("REJECTED")) rejected++;
            else if (r.getStatus().equals("PENDING")) pending++;
        }
        System.out.println("Approved: " + approved + ", Rejected: " + rejected + ", Pending: " + pending);
    }
    void attendanceSummary() {
        System.out.println("--- Attendance Summary ---");
        for (User u : users)
            if (u instanceof Employee)
                System.out.println(u.getName() + ": " + (365 - (u.getTotalLeavesAllowed() - u.getLeaveBalance())) + " days attended this year.");
    }
    void hrFeedbackTable() {
        System.out.println("--- Recent HR Feedback ---");
        if (feedbacks.isEmpty()) System.out.println("No feedback submitted yet.");
        for (HRFeedback fb : feedbacks)
            System.out.println(fb.getEmpName() + ": " + fb.getMessage());
    }

    void awardBoard() {
        if (users.isEmpty()) { printlnInfo("No users yet."); return; }
        User top = users.get(0);
        for (User u : users) if (u.getBadges() > top.getBadges()) top = u;
        System.out.println("🏆 Employee of the Year: " + top.getName() + " (Badges: " + top.getBadges() + ")");
    }
    void policyUpdate() {
        System.out.println("Enter policy update message: ");
        String msg = sc.nextLine();
        System.out.println("Policy announced: " + msg);
    }
    void exportHRFeedback() {
        System.out.println("Export HR Feedback:");
        System.out.println("1. CSV");
        System.out.println("2. TXT");
        System.out.println("3. Show on screen");
        System.out.print("Choose: ");
        String ch = sc.nextLine().trim();
        if (ch.equals("1")) {
            String csv = generateFeedbackCSVString();
            String fname = "hr_feedback_" + timestampForFile() + ".csv";
            saveToFile(fname, csv);
        } else if (ch.equals("2")) {
            String txt = generateFeedbackTXTString();
            String fname = "hr_feedback_" + timestampForFile() + ".txt";
            saveToFile(fname, txt);
        } else hrFeedbackTable();
    }

    // Blockchain/Audit submenu
    void blockchainFeatureMenu() {
        while (true) {
            printlnInfo("Blockchain Features");
            System.out.println("1. Show Leave Blockchain Chain");
            System.out.println("2. Verify Hashes/Integrity");
            System.out.println("3. Audit Trail");
            System.out.println("4. Export Audit Trail (CSV/TXT)");
            System.out.println("5. Back");
            System.out.print("Choose: ");
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1": BlockchainSimulator.printLeaveChain(requests); break;
                case "2": BlockchainSimulator.verifyIntegrity(requests); break;
                case "3": BlockchainSimulator.auditTrail(requests); break;
                case "4":
                    exportBlockchainAudit();
                    break;
                case "5": return;
                default: printlnError("Invalid."); break;
            }
        }
    }
    void exportBlockchainAudit() {
        System.out.println("Export Audit Trail:");
        System.out.println("1. CSV");
        System.out.println("2. TXT");
        System.out.print("Choose: ");
        String ch = sc.nextLine().trim();
        if (ch.equals("1")) {
            String csv = generateBlockchainCSVString();
            String fname = "blockchain_audit_" + timestampForFile() + ".csv";
            saveToFile(fname, csv);
        } else if (ch.equals("2")) {
            String txt = generateBlockchainTXTString();
            String fname = "blockchain_audit_" + timestampForFile() + ".txt";
            saveToFile(fname, txt);
        } else printlnInfo("Cancelled.");
    }

    // Utility
    User getUserById(int empId) {
        for (User u : users) if (u.getEmpId() == empId) return u;
        return null;
    }
}
