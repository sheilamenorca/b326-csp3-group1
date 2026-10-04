package com.joysistvi.CyberAccess.cliview;

import com.joysistvi.CyberAccess.controller.ComputersController;
import com.joysistvi.CyberAccess.controller.RatesController;
import com.joysistvi.CyberAccess.controller.SessionsController;
import com.joysistvi.CyberAccess.controller.TransactionsController;
import com.joysistvi.CyberAccess.controller.UsersController;
import com.joysistvi.CyberAccess.model.Computers;
import com.joysistvi.CyberAccess.model.Rates;
import com.joysistvi.CyberAccess.model.Sessions;
import com.joysistvi.CyberAccess.model.Users;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Console view/controller for computer sessions.
 *
 * Regular PC Rate is open-ended and is billed by actual usage.
 * Promo rates have a fixed package duration and fixed package price.
 */
public class SessionsView {

    private final SessionsController sessionsController;
    private final ComputersController computersController;
    private final RatesController ratesController;
    private final UsersController usersController;
    private final TransactionsController transactionsController;
    private final Scanner scanner;

    private static final Pattern HOURS_PATTERN =
            Pattern.compile("(\\d+)\\s*-?\\s*hour", Pattern.CASE_INSENSITIVE);

    public SessionsView(Scanner scanner) {
        this(scanner, new TransactionsController(
                new com.joysistvi.CyberAccess.service.TransactionsServiceImpl(
                        new com.joysistvi.CyberAccess.repo.TransactionsRepoImpl(
                                new com.joysistvi.CyberAccess.config.DatabaseConnection()))));
    }

    public SessionsView(Scanner scanner, TransactionsController transactionsController) {
        this.scanner = scanner;
        this.sessionsController = new SessionsController();
        this.computersController = new ComputersController();
        this.ratesController = new RatesController();
        this.usersController = new UsersController();
        this.transactionsController = transactionsController;
    }

    public List<Sessions> getAllSessions() throws SQLException {
        return sessionsController.getAll();
    }

    public void update(Sessions session) throws SQLException {
        sessionsController.update(session);
    }

    // =========================================================
    // ADMIN SESSION MENU
    // =========================================================

    public void showMenu() {
        boolean running = true;
        while (running) {
            clearScreen();
            UiHelper.heading("SESSION MANAGEMENT");
            UiHelper.menuItem(1, "Start Session");
            UiHelper.menuItem(2, "View Sessions");
            UiHelper.menuItem(3, "Edit Session");
            UiHelper.menuItem(4, "Remove Session");
            UiHelper.menuItem(0, "Back");
            System.out.println("============================================");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> startSessionForAdmin();
                case "2" -> viewSessions();
                case "3" -> editSession();
                case "4" -> removeSession();
                case "0" -> running = false;
                default -> { System.out.println("Invalid choice."); pause(); }
            }
        }
    }

    private void clearScreen() {
        UiHelper.clearScreen();
    }

    private void startSessionForAdmin() {
        try {
            List<Users> users = usersController.getAll();
            users.removeIf(user -> !Users.STATUS_ACTIVE.equalsIgnoreCase(user.getStatus()));
            if (users.isEmpty()) {
                System.out.println("\nNo active users available.");
                pause();
                return;
            }

            System.out.println("\n================================");
            System.out.println("          SELECT USER");
            System.out.println("================================");
            for (int i = 0; i < users.size(); i++) {
                Users user = users.get(i);
                System.out.printf("%d. %-25s %s%n", i + 1, user.getFullName(), user.getUsername());
            }
            System.out.println("================================");

            int userChoice = readChoice("Select User: ", users.size());
            startSession(users.get(userChoice - 1));
        } catch (SQLException e) {
            System.out.println("\nDatabase error: " + e.getMessage());
            pause();
        }
    }

    public void startUserSession(Users loggedInUser) {
        if (loggedInUser == null) {
            System.out.println("No logged-in user.");
            return;
        }
        startSession(loggedInUser);
    }

    private void startSession(Users selectedUser) {
        try {
            expirePromoSessions();

            for (Sessions existing : sessionsController.getAll()) {
                if (existing.getUserId() == selectedUser.getId()
                        && "Ongoing".equalsIgnoreCase(existing.getStatus())) {
                    System.out.println("\nThis user already has an ongoing session.");
                    System.out.println("Session ID: " + existing.getId());
                    System.out.println("Computer ID: " + existing.getComputerId());
                    pause();
                    return;
                }
            }

            List<Computers> availableComputers =
                    computersController.getByStatus(Computers.STATUS_AVAILABLE);
            if (availableComputers.isEmpty()) {
                System.out.println("\nNo computers are currently available.");
                pause();
                return;
            }

            System.out.println("\n================================");
            System.out.println("       AVAILABLE COMPUTERS");
            System.out.println("================================");
            for (int i = 0; i < availableComputers.size(); i++) {
                Computers computer = availableComputers.get(i);
                System.out.printf("%d. %-15s %s%n", i + 1,
                        computer.getComputerName(), computer.getStatus());
            }
            System.out.println("================================");

            int computerChoice = readChoice("Select Computer: ", availableComputers.size());
            Computers selectedComputer = availableComputers.get(computerChoice - 1);

            List<Rates> rates = ratesController.getByType("Computer");
            rates.removeIf(rate -> !Rates.STATUS_ACTIVE.equalsIgnoreCase(rate.getStatus()));
            if (rates.isEmpty()) {
                System.out.println("\nNo active computer rates available.");
                pause();
                return;
            }

            System.out.println("\n================================");
            System.out.println("         AVAILABLE RATES");
            System.out.println("================================");
            for (int i = 0; i < rates.size(); i++) {
                Rates rate = rates.get(i);
                System.out.printf("%d. %-28s ₱%.2f %s%n", i + 1,
                        rate.getRateName(), rate.getPrice(), rate.getUnit());
            }
            System.out.println("================================");

            int rateChoice = readChoice("Select Rate: ", rates.size());
            Rates selectedRate = rates.get(rateChoice - 1);

            int packageMinutes = getPackageMinutes(selectedRate);

            // 0 means there is no fixed package duration (Regular PC Rate).
            Sessions session = new Sessions(
                    selectedUser.getId(),
                    selectedComputer.getId(),
                    selectedRate.getId(),
                    packageMinutes);

            if (packageMinutes == 0) {
                session.setDurationMinutes(0);
                session.setDurationSeconds(0);
            }

            sessionsController.add(session);
            computersController.changeStatus(
                    selectedComputer.getId(), Computers.STATUS_IN_USE);

            System.out.println("\n================================");
            System.out.println("        SESSION STARTED");
            System.out.println("================================");
            System.out.println("User: " + selectedUser.getFullName());
            System.out.println("Computer: " + selectedComputer.getComputerName());
            System.out.println("Rate: " + selectedRate.getRateName());
            System.out.printf("Price: ₱%.2f%s%n", selectedRate.getPrice(),
                    isPromo(selectedRate) ? " (fixed package price)" : " per hour");
            if (packageMinutes > 0) {
                System.out.println("Package Duration: " + packageMinutes + " minutes");
            } else {
                System.out.println("Duration: Actual usage; session ends when customer chooses End Computer Session.");
            }
            System.out.println("Session ID: " + session.getId());
            System.out.println("Computer is now IN USE.");
            System.out.println("================================");
            System.out.println("Session saved. You may now order food/drinks while the session is ongoing.");
            pause();

        } catch (SQLException e) {
            System.out.println("\nDatabase error: " + e.getMessage());
            pause();
        }
    }

    /** Backward-compatible helper used by the existing admin edit flow. */
    private int getDurationMinutes(Rates rate) {
        return getPackageMinutes(rate);
    }

    /** Returns fixed package minutes; Regular PC Rate returns 0. */
    private int getPackageMinutes(Rates rate) {
        if (!isPromo(rate)) return 0;
        Matcher matcher = HOURS_PATTERN.matcher(rate.getRateName() == null ? "" : rate.getRateName());
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) * 60;
        }
        return 0;
    }

    private boolean isPromo(Rates rate) {
        if (rate == null || rate.getRateName() == null) return false;
        return rate.getRateName().toLowerCase().contains("promo");
    }

    // =========================================================
    // CUSTOMER SESSION ACTIONS
    // =========================================================

    public void viewUserSessionTimer(Users user) {
        try {
            expirePromoSessions();
            Sessions session = findOngoingSession(user.getId());
            UiHelper.heading("SESSION TIMER");

            if (session == null) {
                System.out.println("No ongoing computer session.");
                return;
            }

            Rates rate = session.getRateId() == null ? null : ratesController.getById(session.getRateId());
            long elapsedSeconds = elapsedSeconds(session);

            System.out.println("Session ID : " + session.getId());
            System.out.println("Computer   : " + session.getComputerId());
            System.out.println("Started    : " + session.getStartTime());
            System.out.println("Elapsed    : " + formatTime(elapsedSeconds));

            if (rate != null && isPromo(rate)) {
                long total = getPackageMinutes(rate) * 60L;
                long remaining = Math.max(0, total - elapsedSeconds);
                System.out.println("Remaining  : " + formatTime(remaining));
                System.out.printf("Package    : ₱%.2f%n", rate.getPrice());
            } else if (rate != null) {
                System.out.println("Remaining  : No fixed limit");
                System.out.printf("Current Charge: ₱%.2f%n", calculateCharge(rate, elapsedSeconds));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public void endUserSession(Users user) {
        try {
            expirePromoSessions();
            Sessions session = findOngoingSession(user.getId());
            if (session == null) {
                System.out.println("You do not have an ongoing session.");
                return;
            }

            System.out.print("End computer session #" + session.getId() + "? (Y/N): ");
            String confirmation = scanner.nextLine().trim();
            if (!confirmation.equalsIgnoreCase("Y")) {
                System.out.println("Session was not ended.");
                return;
            }

            completeSession(session, false);
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private Sessions findOngoingSession(int userId) throws SQLException {
        for (Sessions session : sessionsController.getAll()) {
            if (session.getUserId() == userId
                    && "Ongoing".equalsIgnoreCase(session.getStatus())) {
                return session;
            }
        }
        return null;
    }

    private long elapsedSeconds(Sessions session) {
        if (session.getStartTime() == null) return 0;
        return Math.max(0, Duration.between(session.getStartTime(), LocalDateTime.now()).getSeconds());
    }

    /** Completes all promo sessions whose fixed package duration has elapsed. */
    private void expirePromoSessions() throws SQLException {
        for (Sessions session : sessionsController.getAll()) {
            if (!"Ongoing".equalsIgnoreCase(session.getStatus()) || session.getRateId() == null) continue;
            Rates rate = ratesController.getById(session.getRateId());
            int packageMinutes = getPackageMinutes(rate);
            if (packageMinutes <= 0) continue;

            long elapsed = elapsedSeconds(session);
            if (elapsed >= packageMinutes * 60L) {
                completeSession(session, true);
            }
        }
    }

    /**
     * Ends the session, records actual duration, creates a transaction charge,
     * and releases the computer. No database schema changes are required.
     */
    private void completeSession(Sessions session, boolean automatic) throws SQLException {
        Rates rate = session.getRateId() == null ? null : ratesController.getById(session.getRateId());
        LocalDateTime end = LocalDateTime.now();
        long elapsed = Math.max(0, Duration.between(session.getStartTime(), end).getSeconds());

        if (rate != null && isPromo(rate)) {
            int packageSeconds = getPackageMinutes(rate) * 60;
            // A completed promo is capped at its package duration for the recorded usage.
            elapsed = Math.min(elapsed, packageSeconds);
        }

        BigDecimal finalCharge = rate == null
                ? BigDecimal.ZERO
                : calculateCharge(rate, elapsed);

        // Record the actual session first.
        session.setStatus("Completed");
        session.setEndTime(end);
        session.setDurationSeconds((int) Math.min(Integer.MAX_VALUE, elapsed));
        session.setDurationMinutes((int) Math.min(Integer.MAX_VALUE, (elapsed + 59) / 60));
        sessionsController.update(session);

        // Record the computer-session charge as a transaction item.
        if (finalCharge.signum() > 0) {
            int transactionId = transactionsController.create(session.getUserId());
            String description = rate == null
                    ? "Computer Session"
                    : "Computer Session - " + rate.getRateName();
            transactionsController.addCharge(
                    transactionId,
                    description,
                    BigDecimal.ONE,
                    "session",
                    finalCharge.setScale(2, RoundingMode.HALF_UP));
            transactionsController.close(transactionId);
        }

        computersController.changeStatus(session.getComputerId(), Computers.STATUS_AVAILABLE);

        System.out.println();
        System.out.println("============================================");
        System.out.println(automatic ? "     PROMO SESSION COMPLETED" : "     COMPUTER SESSION ENDED");
        System.out.println("============================================");
        System.out.println("Session ID : " + session.getId());
        System.out.println("Time Used  : " + formatTime(elapsed));
        System.out.printf("Final Charge: ₱%.2f%n", finalCharge);
        if (rate != null && isPromo(rate)) {
            System.out.println("Billing     : Fixed promo package price");
        } else {
            System.out.println("Billing     : Actual usage / per-minute rate");
        }
        System.out.println("Computer is now AVAILABLE.");
        System.out.println("============================================");
    }

    private BigDecimal calculateCharge(Rates rate, long elapsedSeconds) {
        if (rate == null || rate.getPrice() == null) return BigDecimal.ZERO;
        if (isPromo(rate)) return rate.getPrice().setScale(2, RoundingMode.HALF_UP);

        // Regular PC Rate: price per hour, prorated to actual seconds used.
        return rate.getPrice()
                .multiply(BigDecimal.valueOf(elapsedSeconds))
                .divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    // =========================================================
    // ADMIN HELPERS (existing functionality)
    // =========================================================

    private void viewSessions() {

        try {

            List<Sessions> sessions =
                    sessionsController.getAll();

            if (sessions.isEmpty()) {

                System.out.println();
                System.out.println(
                        "No sessions found."
                );

                pause();
                return;
            }

            System.out.println();
            System.out.println(
                    "================================================================================"
            );
            System.out.println(
                    "                              ALL SESSIONS"
            );
            System.out.println(
                    "================================================================================"
            );

            System.out.printf(
                    "%-5s %-25s %-15s %-25s %-12s%n",
                    "ID",
                    "User",
                    "Computer",
                    "Rate",
                    "Status"
            );

            System.out.println(
                    "--------------------------------------------------------------------------------"
            );

            for (Sessions session : sessions) {

                // USER NAME
                String userName =
                        "Unknown User";

                Users user =
                        usersController.getById(
                                session.getUserId()
                        );

                if (user != null) {

                    userName =
                            user.getFullName();
                }

                // COMPUTER NAME
                String computerName =
                        "PC-" +
                                String.format(
                                        "%02d",
                                        session.getComputerId()
                                );

                Computers computer =
                        computersController.getById(
                                session.getComputerId()
                        );

                if (computer != null) {

                    computerName =
                            computer.getComputerName();
                }

                // RATE NAME
                String rateName =
                        "Unknown Rate";

                if (session.getRateId() != null) {

                    Rates rate =
                            ratesController.getById(
                                    session.getRateId()
                            );

                    if (rate != null) {

                        rateName =
                                rate.getRateName();
                    }
                }

                System.out.printf(
                        "%-5d %-25s %-15s %-25s %-12s%n",
                        session.getId(),
                        userName,
                        computerName,
                        rateName,
                        session.getStatus()
                );
            }

            System.out.println(
                    "================================================================================"
            );

            pause();

        } catch (SQLException e) {

            System.out.println();
            System.out.println(
                    "Database error: " +
                            e.getMessage()
            );

            pause();
        }
    }

    private void editSession() {

        try {

            System.out.println();
            System.out.println(
                    "================================"
            );
            System.out.println(
                    "          EDIT SESSION"
            );
            System.out.println(
                    "================================"
            );

            System.out.print(
                    "Enter Session ID: "
            );

            String input =
                    scanner.nextLine().trim();

            int sessionId;

            try {

                sessionId =
                        Integer.parseInt(input);

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid Session ID."
                );

                pause();
                return;
            }

            Sessions session =
                    sessionsController.getById(
                            sessionId
                    );

            if (session == null) {

                System.out.println(
                        "Session not found."
                );

                pause();
                return;
            }

            int oldComputerId =
                    session.getComputerId();

            System.out.println();
            System.out.println(
                    "================================"
            );
            System.out.println(
                    "       CURRENT INFORMATION"
            );
            System.out.println(
                    "================================"
            );

            Users currentUser =
                    usersController.getById(
                            session.getUserId()
                    );

            Computers currentComputer =
                    computersController.getById(
                            session.getComputerId()
                    );

            Rates currentRate = null;

            if (session.getRateId() != null) {

                currentRate =
                        ratesController.getById(
                                session.getRateId()
                        );
            }

            System.out.println(
                    "Session ID: " +
                            session.getId()
            );

            System.out.println(
                    "User: " +
                            (currentUser == null
                                    ? "Unknown User"
                                    : currentUser.getFullName())
            );

            System.out.println(
                    "Computer: " +
                            (currentComputer == null
                                    ? "Unknown Computer"
                                    : currentComputer.getComputerName())
            );

            System.out.println(
                    "Rate: " +
                            (currentRate == null
                                    ? "Unknown Rate"
                                    : currentRate.getRateName())
            );

            System.out.println(
                    "Duration: " +
                            (session.getDurationMinutes() == null
                                    ? "-"
                                    : session.getDurationMinutes()
                                    + " minutes")
            );

            System.out.println(
                    "Status: " +
                            session.getStatus()
            );

            System.out.println(
                    "================================"
            );


            System.out.print(
                    "New User ID [" +
                            session.getUserId() +
                            "]: "
            );

            String userInput =
                    scanner.nextLine().trim();

            if (!userInput.isEmpty()) {

                try {

                    int newUserId =
                            Integer.parseInt(
                                    userInput
                            );

                    Users newUser =
                            usersController.getById(
                                    newUserId
                            );

                    if (newUser == null) {

                        System.out.println(
                                "User not found."
                        );

                        pause();
                        return;
                    }

                    if (!Users.STATUS_ACTIVE.equalsIgnoreCase(
                            newUser.getStatus()
                    )) {

                        System.out.println(
                                "Selected user is inactive."
                        );

                        pause();
                        return;
                    }

                    session.setUserId(
                            newUserId
                    );

                } catch (NumberFormatException e) {

                    System.out.println(
                            "Invalid User ID."
                    );

                    pause();
                    return;
                }
            }


            System.out.print(
                    "New Computer ID [" +
                            session.getComputerId() +
                            "]: "
            );

            String computerInput =
                    scanner.nextLine().trim();

            if (!computerInput.isEmpty()) {

                try {

                    int newComputerId =
                            Integer.parseInt(
                                    computerInput
                            );

                    Computers newComputer =
                            computersController.getById(
                                    newComputerId
                            );

                    if (newComputer == null) {

                        System.out.println(
                                "Computer not found."
                        );

                        pause();
                        return;
                    }

                    if (newComputerId != oldComputerId &&
                            !Computers.STATUS_AVAILABLE.equalsIgnoreCase(
                                    newComputer.getStatus()
                            )) {

                        System.out.println(
                                "Selected computer is not available."
                        );

                        pause();
                        return;
                    }

                    session.setComputerId(
                            newComputerId
                    );

                } catch (NumberFormatException e) {

                    System.out.println(
                            "Invalid Computer ID."
                    );

                    pause();
                    return;
                }
            }


            System.out.print(
                    "New Rate ID [" +
                            (session.getRateId() == null
                                    ? "-"
                                    : session.getRateId()) +
                            "]: "
            );

            String rateInput =
                    scanner.nextLine().trim();

            if (!rateInput.isEmpty()) {

                try {

                    int newRateId =
                            Integer.parseInt(
                                    rateInput
                            );

                    Rates newRate =
                            ratesController.getById(
                                    newRateId
                            );

                    if (newRate == null) {

                        System.out.println(
                                "Rate not found."
                        );

                        pause();
                        return;
                    }

                    if (!Rates.STATUS_ACTIVE.equalsIgnoreCase(
                            newRate.getStatus()
                    )) {

                        System.out.println(
                                "Selected rate is inactive."
                        );

                        pause();
                        return;
                    }

                    session.setRateId(
                            newRateId
                    );

                    int newDuration =
                            getDurationMinutes(
                                    newRate
                            );

                    if (newDuration > 0) {

                        session.setDurationMinutes(
                                newDuration
                        );

                        session.setDurationSeconds(
                                newDuration * 60
                        );
                    }

                } catch (NumberFormatException e) {

                    System.out.println(
                            "Invalid Rate ID."
                    );

                    pause();
                    return;
                }
            }

            // duration
            int currentDuration =
                    session.getDurationMinutes() == null
                            ? 0
                            : session.getDurationMinutes();

            System.out.print(
                    "New Duration [" +
                            currentDuration +
                            " minutes, Enter to keep]: "
            );

            String durationInput =
                    scanner.nextLine().trim();

            if (!durationInput.isEmpty()) {

                try {

                    int duration =
                            Integer.parseInt(
                                    durationInput
                            );

                    if (duration <= 0) {

                        System.out.println(
                                "Duration must be greater than 0."
                        );

                        pause();
                        return;
                    }

                    session.setDurationMinutes(
                            duration
                    );

                    session.setDurationSeconds(
                            duration * 60
                    );

                } catch (NumberFormatException e) {

                    System.out.println(
                            "Invalid duration."
                    );

                    pause();
                    return;
                }
            }


            System.out.print(
                    "New Status [" +
                            session.getStatus() +
                            "] (Ongoing/Completed): "
            );

            String statusInput =
                    scanner.nextLine().trim();

            if (!statusInput.isEmpty()) {

                if (statusInput.equalsIgnoreCase(
                        "Ongoing"
                )) {

                    session.setStatus(
                            "Ongoing"
                    );

                } else if (
                        statusInput.equalsIgnoreCase(
                                "Completed"
                        )
                ) {

                    session.setStatus(
                            "Completed"
                    );

                } else {

                    System.out.println(
                            "Invalid status."
                    );

                    pause();
                    return;
                }
            }

            System.out.println();
            System.out.println(
                    "================================"
            );
            System.out.println(
                    "          NEW INFORMATION"
            );
            System.out.println(
                    "================================"
            );

            Users newUser =
                    usersController.getById(
                            session.getUserId()
                    );

            Computers newComputer =
                    computersController.getById(
                            session.getComputerId()
                    );

            Rates newRate = null;

            if (session.getRateId() != null) {

                newRate =
                        ratesController.getById(
                                session.getRateId()
                        );
            }

            System.out.println(
                    "Session ID: " +
                            session.getId()
            );

            System.out.println(
                    "User: " +
                            (newUser == null
                                    ? "Unknown User"
                                    : newUser.getFullName())
            );

            System.out.println(
                    "Computer: " +
                            (newComputer == null
                                    ? "Unknown Computer"
                                    : newComputer.getComputerName())
            );

            System.out.println(
                    "Rate: " +
                            (newRate == null
                                    ? "Unknown Rate"
                                    : newRate.getRateName())
            );

            System.out.println(
                    "Duration: " +
                            session.getDurationMinutes() +
                            " minutes"
            );

            System.out.println(
                    "Status: " +
                            session.getStatus()
            );

            System.out.println(
                    "================================"
            );

            System.out.print(
                    "Save changes? (Y/N): "
            );

            String confirmation =
                    scanner.nextLine().trim();

            if (!confirmation.equalsIgnoreCase("Y")) {

                System.out.println(
                        "Edit cancelled."
                );

                pause();
                return;
            }

            // COMPUTER STATUS

            if (oldComputerId !=
                    session.getComputerId()) {

                computersController.changeStatus(
                        oldComputerId,
                        Computers.STATUS_AVAILABLE
                );
            }

            if ("Ongoing".equalsIgnoreCase(
                    session.getStatus()
            )) {

                computersController.changeStatus(
                        session.getComputerId(),
                        Computers.STATUS_IN_USE
                );

            } else {

                computersController.changeStatus(
                        session.getComputerId(),
                        Computers.STATUS_AVAILABLE
                );

                if (session.getEndTime() == null) {

                    session.setEndTime(
                            LocalDateTime.now()
                    );
                }

                session.setDurationSeconds(0);
            }

            sessionsController.update(
                    session
            );

            System.out.println();
            System.out.println(
                    "Session edited successfully."
            );

        } catch (SQLException e) {

            System.out.println();
            System.out.println(
                    "Database error: " +
                            e.getMessage()
            );
        }

        pause();
    }

    private void removeSession() {

        try {

            System.out.println();
            System.out.println(
                    "================================"
            );
            System.out.println(
                    "          REMOVE SESSION"
            );
            System.out.println(
                    "================================"
            );

            System.out.print(
                    "Enter Session ID: "
            );

            String input =
                    scanner.nextLine().trim();

            int sessionId;

            try {

                sessionId =
                        Integer.parseInt(input);

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid Session ID."
                );

                pause();
                return;
            }

            Sessions session =
                    sessionsController.getById(
                            sessionId
                    );

            if (session == null) {

                System.out.println(
                        "Session not found."
                );

                pause();
                return;
            }

            Users user =
                    usersController.getById(
                            session.getUserId()
                    );

            Computers computer =
                    computersController.getById(
                            session.getComputerId()
                    );

            Rates rate = null;

            if (session.getRateId() != null) {

                rate =
                        ratesController.getById(
                                session.getRateId()
                        );
            }


            // SHOW INFORMATION

            System.out.println();
            System.out.println(
                    "================================"
            );
            System.out.println(
                    "       SESSION INFORMATION"
            );
            System.out.println(
                    "================================"
            );

            System.out.println(
                    "Session ID: " +
                            session.getId()
            );

            System.out.println(
                    "User: " +
                            (user == null
                                    ? "Unknown User"
                                    : user.getFullName())
            );

            System.out.println(
                    "Computer: " +
                            (computer == null
                                    ? "Unknown Computer"
                                    : computer.getComputerName())
            );

            System.out.println(
                    "Rate: " +
                            (rate == null
                                    ? "Unknown Rate"
                                    : rate.getRateName())
            );

            System.out.println(
                    "Status: " +
                            session.getStatus()
            );

            System.out.println(
                    "================================"
            );

            System.out.print(
                    "Are you sure you want to " +
                            "remove this session? (Y/N): "
            );

            String confirmation =
                    scanner.nextLine().trim();

            if (!confirmation.equalsIgnoreCase("Y")) {

                System.out.println(
                        "Remove cancelled."
                );

                pause();
                return;
            }

            // Free computer if session is ongoing
            if ("Ongoing".equalsIgnoreCase(
                    session.getStatus()
            )) {

                computersController.changeStatus(
                        session.getComputerId(),
                        Computers.STATUS_AVAILABLE
                );
            }

            boolean removed =
                    sessionsController.remove(
                            sessionId
                    );

            System.out.println();

            if (removed) {

                System.out.println(
                        "Session removed successfully."
                );

            } else {

                System.out.println(
                        "Session could not be removed."
                );
            }

        } catch (SQLException e) {

            System.out.println();
            System.out.println(
                    "Database error: " +
                            e.getMessage()
            );
        }

        pause();
    }

    private int readChoice(String prompt, int max) {
        while (true) {
            try {
                System.out.print(prompt);
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= 1 && value <= max) return value;
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Invalid choice. Enter a number from 1 to " + max + ".");
        }
    }

    private int readInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid number.");
            }
        }
    }

    private void pause() {
        System.out.println();
        System.out.print("Press ENTER to continue...");
        scanner.nextLine();
    }
}
