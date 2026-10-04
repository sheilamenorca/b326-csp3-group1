package com.joysistvi.CyberAccess;

import com.joysistvi.CyberAccess.cliview.*;
import com.joysistvi.CyberAccess.config.DatabaseConnection;
import com.joysistvi.CyberAccess.controller.*;
import com.joysistvi.CyberAccess.model.*;
import com.joysistvi.CyberAccess.repo.*;
import com.joysistvi.CyberAccess.service.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class app {

    private static final DatabaseConnection DB = new DatabaseConnection();
    private static final Scanner SCANNER = new Scanner(System.in);

    private static final UsersController USERS = new UsersController();
    private static final ComputersController COMPUTERS = new ComputersController();
    private static final RatesController RATES = new RatesController();
    private static final SessionsController SESSIONS = new SessionsController();
    private static final TransactionsController TRANSACTIONS =
            new TransactionsController(new TransactionsServiceImpl(new TransactionsRepoImpl(DB)));
    private static final PaymentsController PAYMENTS =
            new PaymentsController(new PaymentsServiceImpl(new PaymentsRepoImpl(DB)));
    private static final ServicesController SERVICES =
            new ServicesController(new ServicesServiceImpl(new ServicesRepoImpl(DB)));
    private static final FeedbackController FEEDBACK =
            new FeedbackController(new FeedbackServiceImpl(new FeedbackRepoImpl(DB)));
    private static final ConversationController CONVERSATIONS =
            new ConversationController(new ConversationServiceImpl(new ConversationRepoImpl(DB)));
    private static final FoodCatController FOOD_CATEGORIES = new FoodCatController();
    private static final FoodController FOOD = new FoodController();
    private static final DrinkController DRINKS = new DrinkController();
    private static final OrderController ORDERS = new OrderController();
    private static final FoodOrderItemsController FOOD_ORDER_ITEMS =
            new FoodOrderItemsController(new FoodOrderItemsServiceImpl(new FoodOrderItemsRepoImpl(DB)));
    private static final DrinkOrderItemsController DRINK_ORDER_ITEMS =
            new DrinkOrderItemsController(new DrinkOrderItemsServiceImpl(new DrinkOrderItemsRepoImpl(DB)));

    private static final ComputersView COMPUTERS_VIEW = new ComputersView(COMPUTERS, SCANNER);
    private static final RatesView RATES_VIEW = new RatesView(RATES, SCANNER);
    private static final ServicesView SERVICES_VIEW =
            new ServicesView(SERVICES, new ConsoleInput(SCANNER));
    private static final SessionsView SESSIONS_VIEW = new SessionsView(SCANNER, TRANSACTIONS);
    private static final UsersView USERS_VIEW = new UsersView(USERS, SESSIONS_VIEW, SCANNER);
    private static final FeedbackView FEEDBACK_VIEW = new FeedbackView(FEEDBACK, SCANNER);
    private static final BillView BILL_VIEW = new BillView(
            new BillController(new BillServiceImpl(new BillRepoImpl(DB))));
    private static final TransactionsView TRANSACTIONS_VIEW = new TransactionsView(
            TRANSACTIONS,
            BILL_VIEW,
            SERVICES_VIEW,
            new ConsoleInput(SCANNER));
    private static final PaymentsView PAYMENTS_VIEW = new PaymentsView(PAYMENTS);

    public static void main(String[] args) {
        if (!testConnection()) {
            return;
        }

        try {
            while (true) {
                clearScreen();
                printWelcomeMenu();
                int choice = readInt("Choose an option: ");

                switch (choice) {
                    case 1 -> login();
                    case 2 -> register();
                    case 0 -> {
                        clearScreen();
                        System.out.println("Thank you for using CyberAccess. Goodbye!");
                        return;
                    }
                    default -> message("Invalid choice.");
                }
            }
        } finally {
            SCANNER.close();
        }
    }

    private static boolean testConnection() {
        try (Connection ignored = DB.connect()) {
            return true;
        } catch (SQLException e) {
            clearScreen();
            UiHelper.heading("CYBERACCESS DATABASE ERROR");
            System.out.println("Cannot connect to the database.");
            System.out.println("Check XAMPP/MariaDB and DatabaseConnection.java.");
            System.out.println("Error: " + e.getMessage());
            pause();
            return false;
        }
    }

    private static void printWelcomeMenu() {
        UiHelper.banner("CYBERACCESS", "Cyber Cafe Access System");
        UiHelper.menuItem(1, "Log in");
        UiHelper.menuItem(2, "Register Account");
        UiHelper.menuItem(0, "Exit");
        System.out.println();
    }

    private static void login() {
        clearScreen();
        UiHelper.heading("LOG IN");

        String username = readText("Username: ");
        String password = readText("Password: ");
        Users user = USERS.handleLogin(username, password);

        if (user == null) {
            message("Invalid username or password.");
            return;
        }

        message("Login successful. Welcome, " + user.getFullName() + "!");

        if (Users.ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
            adminMenu(user);
        } else {
            customerMenu(user);
        }
    }

    private static void register() {
        clearScreen();
        UiHelper.heading("REGISTER ACCOUNT");

        String fullName = readText("Full Name: ");
        String username = readText("Username: ");
        String password = readText("Password: ");
        String confirm = readText("Confirm Password: ");

        if (!password.equals(confirm)) {
            message("Passwords do not match.");
            return;
        }

        if (fullName.isBlank() || username.isBlank() || password.isBlank()) {
            message("All fields are required.");
            return;
        }

        if (USERS.handleRegister(fullName, username, password)) {
            message("Account registered successfully. You may now log in.");
        } else {
            message("Registration failed. Username may already exist.");
        }
    }

    // =========================================================
    // ADMIN
    // =========================================================

    private static void adminMenu(Users admin) {
        while (true) {
            clearScreen();
            printHeader("ADMIN DASHBOARD");
            UiHelper.welcome(admin.getFullName());
            UiHelper.section("System Administration");
            UiHelper.menuItem(1, "User Management");
            UiHelper.menuItem(2, "Cafe Operations");
            UiHelper.menuItem(3, "Food & Orders");
            UiHelper.menuItem(4, "Transactions & Billing");
            UiHelper.menuItem(5, "Communication");
            UiHelper.menuItem(6, "Reports");
            UiHelper.menuItem(0, "Log Out");
            printDivider();

            switch (readInt("Choose an option: ")) {
                case 1 -> adminUserMenu();
                case 2 -> adminOperationsMenu();
                case 3 -> adminFoodOrderMenu();
                case 4 -> adminTransactionMenu();
                case 5 -> adminCommunicationMenu(admin);
                case 6 -> reports();
                case 0 -> {
                    message("Logged out successfully.");
                    return;
                }
                default -> message("Invalid choice.");
            }
        }
    }

    private static void adminUserMenu() {
        while (true) {
            clearScreen();
            printHeader("USER MANAGEMENT");
            UiHelper.menuItem(1, "Manage Users");
            UiHelper.menuItem(0, "Back");
            printDivider();

            switch (readInt("Choose an option: ")) {
                case 1 -> USERS_VIEW.show();
                case 0 -> { return; }
                default -> message("Invalid choice.");
            }
        }
    }

    private static void adminOperationsMenu() {
        while (true) {
            clearScreen();
            printHeader("CAFE OPERATIONS");
            UiHelper.section("Cafe Operations");
            UiHelper.menuItem(1, "Manage Computers");
            UiHelper.menuItem(2, "Manage Services");
            UiHelper.menuItem(3, "Manage Rates");
            UiHelper.menuItem(4, "Manage Sessions");
            UiHelper.menuItem(0, "Back");
            printDivider();

            switch (readInt("Choose an option: ")) {
                case 1 -> COMPUTERS_VIEW.show();
                case 2 -> SERVICES_VIEW.show();
                case 3 -> RATES_VIEW.show();
                case 4 -> SESSIONS_VIEW.showMenu();
                case 0 -> { return; }
                default -> message("Invalid choice.");
            }
        }
    }

    private static void adminFoodOrderMenu() {
        while (true) {
            clearScreen();
            printHeader("FOOD & ORDERS");
            UiHelper.section("Food & Beverage");
            UiHelper.menuItem(1, "Manage Food Categories");
            UiHelper.menuItem(2, "Manage Food Items");
            UiHelper.menuItem(3, "Manage Drink Items");
            UiHelper.menuItem(4, "View Food & Drink Menu");
            UiHelper.menuItem(5, "Process Food Orders");
            UiHelper.menuItem(6, "View Order Status");
            UiHelper.menuItem(0, "Back");
            printDivider();

            switch (readInt("Choose an option: ")) {
                case 1 -> manageFoodCategories();
                case 2 -> manageFoodItems();
                case 3 -> manageDrinkItems();
                case 4 -> foodMenu();
                case 5 -> processFoodOrders();
                case 6 -> adminOrderStatus();
                case 0 -> { return; }
                default -> message("Invalid choice.");
            }
        }
    }

    private static void adminTransactionMenu() {
        while (true) {
            clearScreen();
            printHeader("TRANSACTIONS & BILLING");
            UiHelper.section("Billing & Payments");
            UiHelper.menuItem(1, "View Unpaid Bills");
            UiHelper.menuItem(2, "Record Payment");
            UiHelper.menuItem(3, "View Paid Transactions");
            UiHelper.menuItem(4, "Transaction History");
            UiHelper.menuItem(0, "Back");
            printDivider();

            switch (readInt("Choose an option: ")) {
                case 1 -> adminUnpaidBills();
                case 2 -> recordPayment();
                case 3 -> adminPaidTransactions();
                case 4 -> adminTransactionHistory();
                case 0 -> { return; }
                default -> message("Invalid choice.");
            }
        }
    }

    private static void adminCommunicationMenu(Users admin) {
        while (true) {
            clearScreen();
            printHeader("COMMUNICATION");
            UiHelper.section("Customer Communication");
            UiHelper.menuItem(1, "Manage Feedback");
            UiHelper.menuItem(2, "Conversations");
            UiHelper.menuItem(0, "Back");
            printDivider();

            switch (readInt("Choose an option: ")) {
                case 1 -> adminFeedbackMenu();
                case 2 -> conversationMenu(admin, true);
                case 0 -> { return; }
                default -> message("Invalid choice.");
            }
        }
    }

    // =========================================================
    // CUSTOMER
    // =========================================================

    private static void customerMenu(Users customer) {
        while (true) {
            clearScreen();
            printHeader("CUSTOMER DASHBOARD");
            UiHelper.welcome(customer.getFullName());
            UiHelper.menuItem(1, "My Account");
            UiHelper.menuItem(2, "Computer Access");
            UiHelper.menuItem(3, "Food & Orders");
            UiHelper.menuItem(4, "My Transactions");
            UiHelper.menuItem(5, "Feedback & Support");
            UiHelper.menuItem(0, "Log Out");
            printDivider();

            switch (readInt("Choose an option: ")) {
                case 1 -> customerAccountMenu(customer);
                case 2 -> customerComputerMenu(customer);
                case 3 -> customerFoodOrderMenu(customer);
                case 4 -> customerTransactionMenu(customer);
                case 5 -> customerCommunicationMenu(customer);
                case 0 -> {
                    message("Logged out successfully.");
                    return;
                }
                default -> message("Invalid choice.");
            }
        }
    }

    private static void customerAccountMenu(Users customer) {
        while (true) {
            clearScreen();
            printHeader("MY ACCOUNT");
            UiHelper.menuItem(1, "View Account Information");
            UiHelper.menuItem(0, "Back");
            printDivider();

            switch (readInt("Choose an option: ")) {
                case 1 -> accountInfo(customer);
                case 0 -> { return; }
                default -> message("Invalid choice.");
            }
        }
    }

    private static void customerComputerMenu(Users customer) {
        while (true) {
            clearScreen();
            printHeader("COMPUTER ACCESS");
            UiHelper.menuItem(1, "View Available Computers");
            UiHelper.menuItem(2, "Start Computer Session");
            UiHelper.menuItem(3, "View Session Timer");
            UiHelper.menuItem(4, "End Computer Session");
            UiHelper.menuItem(0, "Back");
            printDivider();

            switch (readInt("Choose an option: ")) {
                case 1 -> availableComputers();
                case 2 -> startSession(customer);
                case 3 -> viewSessionTimer(customer);
                case 4 -> endSession(customer);
                case 0 -> { return; }
                default -> message("Invalid choice.");
            }
        }
    }

    private static void customerFoodOrderMenu(Users customer) {
        while (true) {
            clearScreen();
            printHeader("FOOD & ORDERS");
            UiHelper.menuItem(1, "View Food Menu");
            UiHelper.menuItem(2, "Order Food");
            UiHelper.menuItem(3, "View Order Status");
            UiHelper.menuItem(0, "Back");
            printDivider();

            switch (readInt("Choose an option: ")) {
                case 1 -> foodMenu();
                case 2 -> orderFood(customer);
                case 3 -> viewOrderStatus(customer);
                case 0 -> { return; }
                default -> message("Invalid choice.");
            }
        }
    }

    private static void customerTransactionMenu(Users customer) {
        while (true) {
            clearScreen();
            printHeader("MY TRANSACTIONS");
            UiHelper.menuItem(1, "View Bill");
            UiHelper.menuItem(2, "View Transaction History");
            UiHelper.menuItem(0, "Back");
            printDivider();

            switch (readInt("Choose an option: ")) {
                case 1 -> viewCustomerBill(customer);
                case 2 -> transactionHistory(customer);
                case 0 -> { return; }
                default -> message("Invalid choice.");
            }
        }
    }

    private static void customerCommunicationMenu(Users customer) {
        while (true) {
            clearScreen();
            printHeader("FEEDBACK & SUPPORT");
            UiHelper.menuItem(1, "Write Feedback");
            UiHelper.menuItem(2, "Conversation");
            UiHelper.menuItem(0, "Back");
            printDivider();

            switch (readInt("Choose an option: ")) {
                case 1 -> writeFeedback(customer);
                case 2 -> conversationMenu(customer, false);
                case 0 -> { return; }
                default -> message("Invalid choice.");
            }
        }
    }

    // =========================================================
    // ADMIN-ONLY LIST/STATUS HELPERS
    // =========================================================

    private static void adminOrderStatus() {
        clearScreen();
        try {
            UiHelper.heading("ORDER STATUS");
            List<Order> orders = ORDERS.getAll();
            if (orders.isEmpty()) {
                UiHelper.empty("No orders found.");
            } else {
                String[][] data = orders.stream()
                        .map(o -> new String[]{"#" + o.getOrderId(), String.valueOf(o.getUserId()), "PHP " + o.getAmount(), o.getStatus()})
                        .toArray(String[][]::new);
                UiHelper.table("Orders", new String[]{"ORDER", "CUSTOMER", "TOTAL", "STATUS"}, data);
            }
        } catch (SQLException e) {
            databaseError(e);
        }
        pause();
    }

    private static void adminUnpaidBills() {
        clearScreen();
        try {
            printHeader("UNPAID BILLS");
            List<Transactions> rows = TRANSACTIONS.history(null).stream()
                    .filter(t -> "Closed".equalsIgnoreCase(t.status()) || "Open".equalsIgnoreCase(t.status()))
                    .toList();
            if (rows.isEmpty()) UiHelper.empty("No unpaid bills found.");
            else {
                String[][] data = rows.stream().map(t -> new String[]{"#" + t.id(), String.valueOf(t.userId()), t.status(), "PHP " + t.total().toPlainString()}).toArray(String[][]::new);
                UiHelper.table("Unpaid Bills", new String[]{"BILL", "CUSTOMER", "STATUS", "AMOUNT DUE"}, data);
            }
        } catch (SQLException e) { databaseError(e); }
        pause();
    }

    private static void adminPaidTransactions() {
        clearScreen();
        try {
            printHeader("PAID TRANSACTIONS");
            List<Transactions> rows = TRANSACTIONS.history(null).stream()
                    .filter(t -> "Paid".equalsIgnoreCase(t.status()))
                    .toList();
            if (rows.isEmpty()) UiHelper.empty("No paid transactions found.");
            else {
                String[][] data = rows.stream().map(t -> new String[]{"#" + t.id(), String.valueOf(t.userId()), "Paid", "PHP " + t.total().toPlainString()}).toArray(String[][]::new);
                UiHelper.table("Paid Transactions", new String[]{"TX #", "CUSTOMER", "STATUS", "TOTAL"}, data);
            }
        } catch (SQLException e) { databaseError(e); }
        pause();
    }

    private static void adminBills() {
        clearScreen();
        try {
            List<Transactions> rows = TRANSACTIONS.history(null);
            UiHelper.table("Bills", new String[]{"TX #", "Customer", "Status", "Total"},
                    rows.stream().map(t -> new String[]{"#" + t.id(), String.valueOf(t.userId()), t.status(), "PHP " + t.total().toPlainString()}).toArray(String[][]::new));
        } catch (SQLException e) {
            databaseError(e);
        }
        pause();
    }

    private static void adminTransactionHistory() {
        clearScreen();
        try {
            List<Transactions> rows = TRANSACTIONS.history(null);
            UiHelper.table("Transaction History", new String[]{"TX #", "Customer", "Date", "Status", "Total"},
                    rows.stream().map(t -> new String[]{"#" + t.id(), String.valueOf(t.userId()), String.valueOf(t.createdAt()), t.status(), "PHP " + t.total().toPlainString()}).toArray(String[][]::new));
        } catch (SQLException e) {
            databaseError(e);
        }
        pause();
    }

    private static void accountInfo(Users user) {
        clearScreen();
        UiHelper.heading("ACCOUNT INFORMATION");
        System.out.println("User ID : " + user.getId());
        System.out.println("Name    : " + user.getFullName());
        System.out.println("Username: " + user.getUsername());
        System.out.println("Role    : " + user.getRole());
        System.out.println("Status  : " + user.getStatus());
        System.out.println("Created : " + user.getCreatedAt());
        pause();
    }

    private static void availableComputers() {
        clearScreen();
        try {
            List<Computers> rows = COMPUTERS.getByStatus(Computers.STATUS_AVAILABLE);
            if (rows.isEmpty()) {
                UiHelper.empty("No computers are currently available.");
            } else {
                String[][] data = rows.stream()
                        .map(c -> new String[]{String.valueOf(c.getId()), c.getComputerName(), c.getStatus()})
                        .toArray(String[][]::new);
                UiHelper.table("Available Computers", new String[]{"ID", "Computer", "Status"}, data);
            }
        } catch (SQLException e) {
            databaseError(e);
        }
        pause();
    }

    private static void startSession(Users customer) {
        clearScreen();
        SESSIONS_VIEW.startUserSession(customer);
    }

    private static void viewSessionTimer(Users customer) {
        clearScreen();
        SESSIONS_VIEW.viewUserSessionTimer(customer);
        pause();
    }

    private static void endSession(Users customer) {
        clearScreen();
        SESSIONS_VIEW.endUserSession(customer);
        pause();
    }

    private static boolean hasOngoingSession(int userId) throws SQLException {
        return findOngoingSession(userId) != null;
    }

    private static Sessions findOngoingSession(int userId) throws SQLException {
        for (Sessions s : SESSIONS.getAll()) {
            if (s.getUserId() == userId && "Ongoing".equalsIgnoreCase(s.getStatus())) return s;
        }
        return null;
    }

    // =========================================================
    // FOOD / DRINK
    // =========================================================

    private static void foodMenu() {
        clearScreen();
        try {
            List<Food> foodRows = FOOD.getAll();
            String[][] foodData = new String[foodRows.size()][];
            for (int i = 0; i < foodData.length; i++) {
                Food f = foodRows.get(i);
                foodData[i] = new String[]{String.valueOf(f.getId()), f.getFood(), categoryName(f.getCategoryId()), String.format("PHP %.2f", f.getPrice())};
            }
            UiHelper.table("Food Menu", new String[]{"ID", "Food", "Category", "Price"}, foodData);
            System.out.println();
            List<DrinkItem> drinkRows = DRINKS.getAll();
            String[][] drinkData = new String[drinkRows.size()][];
            for (int i = 0; i < drinkData.length; i++) {
                DrinkItem d = drinkRows.get(i);
                drinkData[i] = new String[]{String.valueOf(d.getId()), d.getName(), categoryName(d.getCategoryId()), String.format("PHP %.2f", d.getPrice())};
            }
            UiHelper.table("Drink Menu", new String[]{"ID", "Drink", "Category", "Price"}, drinkData);
        } catch (SQLException e) { databaseError(e); }
        pause();
    }

    private static String categoryName(int id) throws SQLException {
        var c = FOOD_CATEGORIES.getById(id);
        return c == null ? "Category #" + id : c.getName();
    }

    private static void manageFoodItems() {
        while (true) {
            clearScreen();
            UiHelper.heading("MANAGE FOOD ITEMS");
            UiHelper.menuItem(1, "List Food");
            UiHelper.menuItem(2, "Add Food");
            UiHelper.menuItem(3, "Edit Food");
            UiHelper.menuItem(4, "Delete Food");
            UiHelper.menuItem(0, "Back");
            try {
                switch (readInt("Choose an option: ")) {
                    case 1 -> { for (Food f : FOOD.getAll()) System.out.printf("#%d | %s | Category %d | PHP %.2f%n", f.getId(), f.getFood(), f.getCategoryId(), f.getPrice()); pause(); }
                    case 2 -> {
                        String name = readText("Food name: "); int cat = readInt("Category ID: "); BigDecimal price = readDecimal("Price (PHP): ");
                        FOOD.add(new Food(name, cat, price)); message("Food item added.");
                    }
                    case 3 -> {
                        int id = readInt("Food ID: "); Food f = FOOD.getById(id);
                        if (f == null) { message("Food item not found."); break; }
                        String name = readText("Name [" + f.getFood() + "]: "); if (!name.isBlank()) f.setFood(name);
                        String catText = readText("Category ID [" + f.getCategoryId() + "]: "); if (!catText.isBlank()) f.setCategoryId(Integer.parseInt(catText));
                        String priceText = readText("Price [" + f.getPrice() + "]: "); if (!priceText.isBlank()) f.setPrice(new BigDecimal(priceText));
                        message(FOOD.update(f) ? "Food updated." : "Food not updated.");
                    }
                    case 4 -> { int id = readInt("Food ID: "); message(FOOD.delete(id) ? "Food deleted." : "Food could not be deleted. It may already be used in an order."); }
                    case 0 -> { return; }
                    default -> message("Invalid choice.");
                }
            } catch (SQLException e) { databaseError(e); } catch (IllegalArgumentException e) { message(e.getMessage()); }
        }
    }

    private static void manageDrinkItems() {
        while (true) {
            clearScreen();
            UiHelper.heading("MANAGE DRINK ITEMS");
            UiHelper.menuItem(1, "List Drinks");
            UiHelper.menuItem(2, "Add Drink");
            UiHelper.menuItem(3, "Edit Drink");
            UiHelper.menuItem(4, "Delete Drink");
            UiHelper.menuItem(0, "Back");
            try {
                switch (readInt("Choose an option: ")) {
                    case 1 -> { for (DrinkItem d : DRINKS.getAll()) System.out.printf("#%d | %s | Category %d | PHP %.2f%n", d.getId(), d.getName(), d.getCategoryId(), d.getPrice()); pause(); }
                    case 2 -> { String name=readText("Drink name: "); int cat=readInt("Category ID: "); BigDecimal price=readDecimal("Price (PHP): "); DRINKS.add(new DrinkItem(name,cat,price)); message("Drink item added."); }
                    case 3 -> {
                        int id=readInt("Drink ID: "); DrinkItem d=DRINKS.getById(id); if(d==null){message("Drink not found.");break;}
                        String name=readText("Name ["+d.getName()+"]: "); if(!name.isBlank())d.setName(name);
                        String cat=readText("Category ID ["+d.getCategoryId()+"]: "); if(!cat.isBlank())d.setCategoryId(Integer.parseInt(cat));
                        String price=readText("Price ["+d.getPrice()+"]: "); if(!price.isBlank())d.setPrice(new BigDecimal(price));
                        message(DRINKS.update(d)?"Drink updated.":"Drink not updated.");
                    }
                    case 4 -> { int id=readInt("Drink ID: "); message(DRINKS.delete(id)?"Drink deleted.":"Drink could not be deleted. It may already be used in an order."); }
                    case 0 -> { return; }
                    default -> message("Invalid choice.");
                }
            } catch (SQLException e) { databaseError(e); } catch (IllegalArgumentException e) { message(e.getMessage()); }
        }
    }

    private static void orderFood(Users customer) {
        while (true) {
            clearScreen();
            UiHelper.heading("ORDER FOOD");
            UiHelper.menuItem(1, "New Order");
            UiHelper.menuItem(2, "Add Food Item to Existing Order");
            UiHelper.menuItem(3, "Add Drink Item to Existing Order");
            UiHelper.menuItem(0, "Back");

            switch (readInt("Choose an option: ")) {
                case 1 -> createFoodOrder(customer);
                case 2 -> addFoodToOrder(customer);
                case 3 -> addDrinkToOrder(customer);
                case 0 -> { return; }
                default -> message("Invalid choice.");
            }
        }
    }

    private static void createFoodOrder(Users customer) {
        clearScreen();
        try {
            Order order = new Order(customer.getId());
            ORDERS.create(order);
            message("Order created successfully. Order #" + order.getOrderId());
            addItemsToNewOrder(order.getOrderId());
        } catch (SQLException e) {
            databaseError(e);
        }
    }

    private static void addItemsToNewOrder(int orderId) {
        while (true) {
            System.out.println();
            UiHelper.menuItem(1, "Add Food");
            UiHelper.menuItem(2, "Add Drink");
            UiHelper.menuItem(0, "Finish Order");
            switch (readInt("Choose an option: ")) {
                case 1 -> addFoodItem(orderId);
                case 2 -> addDrinkItem(orderId);
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void addFoodToOrder(Users customer) {
        int orderId = selectCustomerOrder(customer);
        if (orderId > 0) addFoodItem(orderId);
    }

    private static void addDrinkToOrder(Users customer) {
        int orderId = selectCustomerOrder(customer);
        if (orderId > 0) addDrinkItem(orderId);
    }

    private static int selectCustomerOrder(Users customer) {
        try {
            List<Order> orders = ORDERS.getByUser(customer.getId());
            if (orders.isEmpty()) {
                message("You have no orders yet.");
                return -1;
            }
            String[][] orderData = orders.stream()
                    .map(o -> new String[]{"#" + o.getOrderId(), "PHP " + o.getAmount(), o.getStatus()})
                    .toArray(String[][]::new);
            UiHelper.table("Your Orders", new String[]{"Order", "Amount", "Status"}, orderData);
            return readInt("Order ID: ");
        } catch (SQLException e) {
            databaseError(e);
            return -1;
        }
    }

    private static void addFoodItem(int orderId) {
        try {
            Order order=ORDERS.getById(orderId);
            if(order==null || !"Pending".equalsIgnoreCase(order.getStatus())) { message("Only Pending orders can be modified."); return; }
            List<Food> foods=FOOD.getAll();
            UiHelper.table("Food Items", new String[]{"ID", "Food", "Price"},
                    foods.stream().map(f -> new String[]{String.valueOf(f.getId()), f.getFood(), String.format("PHP %.2f", f.getPrice())}).toArray(String[][]::new));
            int foodId=readInt("Food item ID: "); Food f=FOOD.getById(foodId);
            if(f==null){message("Food item not found.");return;}
            int quantity=readInt("Quantity: ");
            if(quantity<=0){message("Quantity must be greater than zero.");return;}
            boolean ok=FOOD_ORDER_ITEMS.handleAdd(new FoodOrderItems(orderId,foodId,quantity,f.getPrice().doubleValue()));
            message(ok?String.format("Added %dx %s. Unit price: PHP %.2f",quantity,f.getFood(),f.getPrice()):"Could not add food item.");
        }catch(SQLException e){databaseError(e);}
    }

    private static void addDrinkItem(int orderId) {
        try {
            Order order=ORDERS.getById(orderId);
            if(order==null || !"Pending".equalsIgnoreCase(order.getStatus())) { message("Only Pending orders can be modified."); return; }
            List<DrinkItem> drinks=DRINKS.getAll();
            UiHelper.table("Drink Items", new String[]{"ID", "Drink", "Price"},
                    drinks.stream().map(d -> new String[]{String.valueOf(d.getId()), d.getName(), String.format("PHP %.2f", d.getPrice())}).toArray(String[][]::new));
            int drinkId=readInt("Drink item ID: "); DrinkItem d=DRINKS.getById(drinkId);
            if(d==null){message("Drink item not found.");return;}
            int quantity=readInt("Quantity: ");
            if(quantity<=0){message("Quantity must be greater than zero.");return;}
            boolean ok=DRINK_ORDER_ITEMS.add(new DrinkOrderItems(orderId,drinkId,quantity,d.getPrice().doubleValue()));
            message(ok?String.format("Added %dx %s. Unit price: PHP %.2f",quantity,d.getName(),d.getPrice()):"Could not add drink item.");
        }catch(SQLException e){databaseError(e);}
    }

    private static void viewOrderStatus(Users customer) {
        clearScreen();
        try {
            List<Order> orders = ORDERS.getByUser(customer.getId());
            UiHelper.table("Order Status", new String[]{"Order", "Amount", "Status"},
                    orders.stream().map(o -> new String[]{"#" + o.getOrderId(), "PHP " + o.getAmount(), o.getStatus()}).toArray(String[][]::new));
        } catch (SQLException e) {
            databaseError(e);
        }
        pause();
    }

    // =========================================================
    // TRANSACTIONS / PAYMENTS
    // =========================================================

    private static void transactionHistory(Users customer) {
        clearScreen();
        try {
            List<Transactions> rows = TRANSACTIONS.history(customer.getId());
            UiHelper.table("Transaction History", new String[]{"TX #", "Date", "Status", "Total"},
                    rows.stream().map(t -> new String[]{"#" + t.id(), String.valueOf(t.createdAt()), t.status(), "PHP " + t.total().toPlainString()}).toArray(String[][]::new));
        } catch (SQLException e) {
            databaseError(e);
        }
        pause();
    }

    private static void viewCustomerBill(Users customer) {
        clearScreen();
        try {
            List<Transactions> rows = TRANSACTIONS.history(customer.getId());
            if (rows.isEmpty()) {
                message("No transaction bills found.");
                return;
            }
            UiHelper.table("My Bills", new String[]{"TX #", "Date", "Status", "Total"},
                    rows.stream().map(t -> new String[]{"#" + t.id(), String.valueOf(t.createdAt()), t.status(), "PHP " + t.total().toPlainString()}).toArray(String[][]::new));
            int id = readInt("Transaction ID to view: ");
            Transactions selected = TRANSACTIONS.history(customer.getId()).stream()
                    .filter(t -> t.id() == id).findFirst().orElse(null);
            if (selected == null) {
                message("That transaction does not belong to your account.");
                return;
            }
            clearScreen();
            BILL_VIEW.show(id);
            pause();
        } catch (SQLException e) {
            databaseError(e);
        }
    }

    private static void recordPayment() {
        clearScreen();
        try {
            UiHelper.heading("RECORD PAYMENT");
            List<Transactions> unpaid = TRANSACTIONS.history(null).stream()
                    .filter(t -> "Closed".equalsIgnoreCase(t.status()) || "Open".equalsIgnoreCase(t.status()))
                    .toList();
            if (unpaid.isEmpty()) { System.out.println("No unpaid bills found."); pause(); return; }
            UiHelper.table("Unpaid Bills", new String[]{"TX #", "Customer", "Status", "Amount Due"},
                    unpaid.stream().map(t -> new String[]{"#" + t.id(), String.valueOf(t.userId()), t.status(), "PHP " + t.total().toPlainString()}).toArray(String[][]::new));
            int transactionId = readInt("Transaction ID: ");
            Transactions selected = unpaid.stream().filter(t -> t.id()==transactionId).findFirst().orElse(null);
            if (selected == null) { message("Transaction not found or already paid."); return; }
            BigDecimal amount = readDecimal("Amount received: ");
            if (amount.signum() <= 0) { message("Amount received must be greater than zero."); return; }
            if (amount.compareTo(selected.total()) < 0) {
                message("Insufficient payment. Amount due is PHP " + selected.total().toPlainString() + "."); return;
            }
            String method = readText("Payment method (Cash/Card/GCash): ");
            if (method.isBlank()) method = "Cash";
            Payments payment = PAYMENTS.add(transactionId, amount, method, "PAID");
            TRANSACTIONS.markPaid(transactionId);
            BigDecimal change = amount.subtract(selected.total());
            clearScreen();
            printHeader("PAYMENT RECEIPT");
            System.out.println("Transaction : #" + transactionId);
            System.out.println("Amount Due  : PHP " + selected.total().toPlainString());
            System.out.println("Amount Paid : PHP " + amount.toPlainString());
            System.out.println("Change      : PHP " + change.toPlainString());
            System.out.println("Method      : " + method);
            System.out.println("Status      : PAID");
            System.out.println("Payment ID  : " + payment.getId());
        } catch (SQLException e) { databaseError(e); }
        pause();
    }

    private static void adminFeedbackMenu() {
        while (true) {
            clearScreen();
            printHeader("FEEDBACK MANAGEMENT");
            UiHelper.menuItem(1, "View All Feedback");
            UiHelper.menuItem(2, "Delete Feedback");
            UiHelper.menuItem(0, "Back");
            printDivider();
            try {
                switch (readInt("Choose an option: ")) {
                    case 1 -> {
                        List<Feedback> rows = FEEDBACK.handleViewAllFeedback();
                        clearScreen(); printHeader("CUSTOMER FEEDBACK");
                        if (rows.isEmpty()) UiHelper.empty("No feedback found.");
                        else {
                            String[][] data = rows.stream()
                                    .map(f -> new String[]{"#" + f.getId(), String.valueOf(f.getUserId()), f.getComment()})
                                    .toArray(String[][]::new);
                            UiHelper.table("Feedback", new String[]{"ID", "CUSTOMER", "FEEDBACK"}, data);
                        }
                        pause();
                    }
                    case 2 -> {
                        int id=readInt("Feedback ID: ");
                        message(FEEDBACK.handleDeleteFeedback(id) ? "Feedback deleted." : "Feedback not found.");
                    }
                    case 0 -> { return; }
                    default -> message("Invalid choice.");
                }
            } catch (Exception e) { message("Could not process feedback: " + e.getMessage()); }
        }
    }

    // =========================================================
    // FEEDBACK / CONVERSATIONS
    // =========================================================

    private static void writeFeedback(Users customer) {
        clearScreen();
        String comment = readText("Write your feedback: ");
        if (comment.isBlank()) {
            message("Feedback cannot be empty.");
            return;
        }
        boolean ok = FEEDBACK.handleAddFeedback(new Feedback(comment, customer.getId()));
        message(ok ? "Thank you. Your feedback was recorded." : "Could not save feedback.");
    }

    private static void conversationMenu(Users currentUser, boolean admin) {
        while (true) {
            clearScreen();
            printHeader("CONVERSATION");
            if (admin) {
                UiHelper.menuItem(1, "View All Messages");
                UiHelper.menuItem(2, "View Customer Conversation");
                UiHelper.menuItem(3, "Reply to Customer");
                UiHelper.menuItem(4, "Close Conversation");
            } else {
                UiHelper.menuItem(1, "View My Conversation");
                UiHelper.menuItem(2, "Send Message");
            }
            UiHelper.menuItem(0, "Back");

            int choice = readInt("Choose an option: ");
            try {
                if (!admin) {
                    switch (choice) {
                        case 1 -> showConversations(CONVERSATIONS.getByUser(currentUser.getId()));
                        case 2 -> sendConversation(currentUser, false);
                        case 0 -> { return; }
                        default -> message("Invalid choice.");
                    }
                } else {
                    switch (choice) {
                        case 1 -> showConversations(CONVERSATIONS.getAll());
                        case 2 -> showConversations(CONVERSATIONS.getByUser(readInt("User ID: ")));
                        case 3 -> sendConversation(currentUser, true);
                        case 4 -> {
                            int id = readInt("Conversation ID: ");
                            message(CONVERSATIONS.setStatus(id, "Closed") ? "Conversation closed." : "Conversation not found.");
                        }
                        case 0 -> { return; }
                        default -> message("Invalid choice.");
                    }
                }
            } catch (SQLException e) {
                databaseError(e);
            }
        }
    }

    private static void showConversations(List<Conversation> rows) {
        clearScreen();
        printHeader("CONVERSATION MESSAGES");
        if (rows.isEmpty()) {
            UiHelper.empty("No conversation messages found.");
        } else {
            String[][] data = rows.stream()
                    .map(c -> new String[]{"#" + c.getId(), String.valueOf(c.getUserId()), c.getSenderType(), c.getStatus(), c.getCreatedAt() == null ? "" : c.getCreatedAt().toString(), c.getMessage()})
                    .toArray(String[][]::new);
            UiHelper.table("Conversations", new String[]{"ID", "CUSTOMER", "SENDER", "STATUS", "DATE", "MESSAGE"}, data);
        }
        pause();
    }

    private static void sendConversation(Users currentUser, boolean admin) throws SQLException {
        int userId = admin ? readInt("Customer/User ID: ") : currentUser.getId();
        String message = readText("Message: ");
        if (message.isBlank()) {
            message("Message cannot be empty.");
            return;
        }
        String sender = admin ? "ADMIN" : "CUSTOMER";
        Conversation c = new Conversation(userId, message, sender);
        message(CONVERSATIONS.send(c) ? "Message sent." : "Message could not be sent.");
    }

    // =========================================================
    // FOOD CATEGORY / ORDERS (ADMIN)
    // =========================================================

    private static void manageFoodCategories() {
        while (true) {
            clearScreen();
            printHeader("FOOD CATEGORY MANAGEMENT");
            UiHelper.menuItem(1, "View Categories");
            UiHelper.menuItem(2, "Add Category");
            UiHelper.menuItem(3, "Update Category");
            UiHelper.menuItem(4, "Delete Category");
            UiHelper.menuItem(0, "Back");
            try {
                switch (readInt("Choose an option: ")) {
                    case 1 -> {
                        var rows = FOOD_CATEGORIES.getAll();
                        if (rows.isEmpty()) UiHelper.empty("No categories found.");
                        else {
                            String[][] data = rows.stream().map(c -> new String[]{"#" + c.getId(), c.getName()}).toArray(String[][]::new);
                            UiHelper.table("Food Categories", new String[]{"ID", "CATEGORY"}, data);
                        }
                        pause();
                    }
                    case 2 -> {
                        FOOD_CATEGORIES.add(new com.joysistvi.CyberAccess.model.FoodCat(readText("Category name: ")));
                        message("Category added.");
                    }
                    case 3 -> {
                        int id = readInt("Category ID: ");
                        com.joysistvi.CyberAccess.model.FoodCat c = FOOD_CATEGORIES.getById(id);
                        if (c == null) { message("Category not found."); break; }
                        c.setName(readText("New name: "));
                        message(FOOD_CATEGORIES.update(c) ? "Category updated." : "Category not updated.");
                    }
                    case 4 -> {
                        int id = readInt("Category ID: ");
                        message(FOOD_CATEGORIES.delete(id) ? "Category deleted." : "Category could not be deleted.");
                    }
                    case 0 -> { return; }
                    default -> message("Invalid choice.");
                }
            } catch (SQLException e) {
                databaseError(e);
            }
        }
    }

    private static void processFoodOrders() {
        while (true) {
            clearScreen();
            printHeader("PROCESS FOOD ORDERS");
            UiHelper.menuItem(1, "View All Orders");
            UiHelper.menuItem(2, "View Order Items");
            UiHelper.menuItem(3, "Change Order Status");
            UiHelper.menuItem(0, "Back");
            try {
                switch (readInt("Choose an option: ")) {
                    case 1 -> {
                        var rows = ORDERS.getAll();
                        if (rows.isEmpty()) UiHelper.empty("No orders found.");
                        else {
                            String[][] data = rows.stream().map(o -> new String[]{"#" + o.getOrderId(), String.valueOf(o.getUserId()), "PHP " + o.getAmount(), o.getStatus()}).toArray(String[][]::new);
                            UiHelper.table("Orders", new String[]{"ORDER", "CUSTOMER", "TOTAL", "STATUS"}, data);
                        }
                        pause();
                    }
                    case 2 -> viewOrderItemsAdmin();
                    case 3 -> {
                        int id = readInt("Order ID: ");
                        String status = readText("Status (Pending/Preparing/Completed/Cancelled): ");
                        if (!List.of("Pending","Preparing","Completed","Cancelled").contains(status)) { message("Invalid order status."); break; }
                        Order order = ORDERS.getById(id);
                        if (order == null) { message("Order not found."); break; }
                        if ("Completed".equalsIgnoreCase(status) && !"Completed".equalsIgnoreCase(order.getStatus())) {
                            int txId = createOrderTransaction(order);
                            if (txId <= 0) { message("Order was not completed because its transaction could not be created."); break; }
                            order.setTransactionId(txId);
                        }
                        message(ORDERS.update(new Order(order.getOrderId(), order.getTransactionId(), order.getUserId(), order.getAmount(), status)) ? "Order status updated." : "Order not updated.");
                    }
                    case 0 -> { return; }
                    default -> message("Invalid choice.");
                }
            } catch (SQLException e) {
                databaseError(e);
            }
        }
    }

    private static int createOrderTransaction(Order order) throws SQLException {
        if (order.getTransactionId() != null) return order.getTransactionId();
        int txId = TRANSACTIONS.create(order.getUserId());
        for (FoodOrderItems i : FOOD_ORDER_ITEMS.handleGetByOrderId(order.getOrderId())) {
            Food f = FOOD.getById(i.getFoodItemId());
            String desc = f == null ? "Food item #" + i.getFoodItemId() : f.getFood();
            TRANSACTIONS.addCharge(txId, desc, BigDecimal.valueOf(i.getQuantity()), "piece", BigDecimal.valueOf(i.getUnitPrice()));
        }
        for (DrinkOrderItems i : DRINK_ORDER_ITEMS.getByOrderId(order.getOrderId())) {
            DrinkItem d = DRINKS.getById(i.getDrinkItemId());
            String desc = d == null ? "Drink item #" + i.getDrinkItemId() : d.getName();
            TRANSACTIONS.addCharge(txId, desc, BigDecimal.valueOf(i.getQuantity()), "piece", BigDecimal.valueOf(i.getUnitPrice()));
        }
        TRANSACTIONS.close(txId);
        return txId;
    }

    private static void viewOrderItemsAdmin() throws SQLException {
        int orderId = readInt("Order ID: ");
        clearScreen();
        printHeader("ORDER #" + orderId + " ITEMS");
        var foods = FOOD_ORDER_ITEMS.handleGetByOrderId(orderId);
        var drinks = DRINK_ORDER_ITEMS.getByOrderId(orderId);
        UiHelper.section("Food Items");
        if (foods.isEmpty()) UiHelper.empty("No food items.");
        else {
            String[][] data = new String[foods.size()][4];
            for (int n = 0; n < foods.size(); n++) {
                FoodOrderItems i = foods.get(n);
                Food f = FOOD.getById(i.getFoodItemId());
                data[n] = new String[]{"#" + i.getId(), f == null ? "Food #" + i.getFoodItemId() : f.getFood(), String.valueOf(i.getQuantity()), "PHP " + String.format("%.2f", i.getUnitPrice())};
            }
            UiHelper.table("Order Items", new String[]{"ID", "ITEM", "QTY", "UNIT PRICE"}, data);
        }
        UiHelper.section("Drink Items");
        if (drinks.isEmpty()) UiHelper.empty("No drink items.");
        else {
            String[][] data = new String[drinks.size()][4];
            for (int n = 0; n < drinks.size(); n++) {
                DrinkOrderItems i = drinks.get(n);
                DrinkItem d = DRINKS.getById(i.getDrinkItemId());
                data[n] = new String[]{"#" + i.getId(), d == null ? "Drink #" + i.getDrinkItemId() : d.getName(), String.valueOf(i.getQuantity()), "PHP " + String.format("%.2f", i.getUnitPrice())};
            }
            UiHelper.table("Order Items", new String[]{"ID", "ITEM", "QTY", "UNIT PRICE"}, data);
        }
        pause();
    }

    // =========================================================
    // REPORTS
    // =========================================================

    private static void reports() {
        clearScreen();
        printHeader("REPORTS");
        try (Connection c = DB.connect()) {
            System.out.println("Users             : " + count(c, "users"));
            System.out.println("Computers         : " + count(c, "computers"));
            System.out.println("Services          : " + count(c, "services"));
            System.out.println("Rates             : " + count(c, "rates"));
            System.out.println("Sessions          : " + count(c, "sessions"));
            System.out.println("Orders            : " + count(c, "orders"));
            System.out.println("Transactions      : " + count(c, "transactions"));
            System.out.println("Payments          : " + count(c, "payments"));
            System.out.println("Feedback          : " + count(c, "feedback"));
            System.out.println("Conversations     : " + count(c, "conversations"));
            System.out.println("Paid Revenue      : PHP " + scalar(c,
                    "SELECT COALESCE(SUM(amount),0) FROM payments WHERE UPPER(status)='PAID'"));
        } catch (SQLException e) {
            databaseError(e);
        }
        pause();
    }

    private static long count(Connection c, String table) throws SQLException {
        try (var s = c.createStatement(); var r = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            r.next();
            return r.getLong(1);
        }
    }

    private static String scalar(Connection c, String sql) throws SQLException {
        try (var s = c.createStatement(); var r = s.executeQuery(sql)) {
            r.next();
            return r.getString(1);
        }
    }

    // =========================================================
    // CONSOLE HELPERS
    // =========================================================

    private static void printHeader(String title) {
        UiHelper.title(title);
    }

    private static void printDivider() {
        UiHelper.divider();
    }

    private static String readText(String prompt) {
        UiHelper.prompt(prompt);
        return SCANNER.nextLine().trim();
    }

    private static int readInt(String prompt) {
        while (true) {
            UiHelper.prompt(prompt);
            String value = SCANNER.nextLine().trim();
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                UiHelper.error("Please enter a valid number.");
            }
        }
    }

    private static BigDecimal readDecimal(String prompt) {
        while (true) {
            UiHelper.prompt(prompt);
            String value = SCANNER.nextLine().trim();
            try {
                BigDecimal result = new BigDecimal(value);
                if (result.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
                return result;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid amount.");
            }
        }
    }

    private static boolean confirm(String prompt) {
        return readText(prompt + " (Y/N): ").equalsIgnoreCase("Y");
    }

    private static void message(String text) {
        System.out.println();
        if (text != null && (text.toLowerCase().contains("success") || text.toLowerCase().contains("recorded") || text.toLowerCase().contains("updated") || text.toLowerCase().contains("deleted") || text.toLowerCase().contains("added") || text.toLowerCase().contains("sent"))) {
            UiHelper.success(text);
        } else {
            UiHelper.info(text);
        }
        pause();
    }

    private static void databaseError(SQLException e) {
        System.out.println();
        UiHelper.error("Database error: " + e.getMessage());
        pause();
    }

    private static void pause() {
        System.out.print("\nPress Enter to continue...");
        SCANNER.nextLine();
    }

    private static String formatDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private static void clearScreen() {
        UiHelper.clearScreen();
    }
}
