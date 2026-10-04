package com.joysistvi.CyberAccess.cliview;

import com.joysistvi.CyberAccess.controller.ComputersController;
import com.joysistvi.CyberAccess.model.Computers;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Scanner;

public class ComputersView {

    private static final String[] STATUSES = {
            Computers.STATUS_AVAILABLE, Computers.STATUS_IN_USE, Computers.STATUS_MAINTENANCE};

    private final ComputersController controller;
    private final Scanner scanner;

    public ComputersView() {
        this(new ComputersController(), new Scanner(System.in));
    }

    // Lets the main menu share one Scanner across all views
    public ComputersView(ComputersController controller, Scanner scanner) {
        this.controller = controller;
        this.scanner = scanner;
    }

    public void show() {
        boolean running = true;
        while (running) {
            clearScreen();
            System.out.println();
            System.out.println("===== COMPUTERS =====");
            UiHelper.menuItem(1, "View all computers");
            UiHelper.menuItem(2, "Find computer by ID");
            UiHelper.menuItem(3, "View computers by status");
            UiHelper.menuItem(4, "Add computer");
            UiHelper.menuItem(5, "Edit computer");
            UiHelper.menuItem(6, "Change computer status");
            UiHelper.menuItem(7, "Delete computer");
            UiHelper.menuItem(0, "Back");
            System.out.print("Choose: ");

            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> printList(controller.getAll());
                    case "2" -> findById();
                    case "3" -> viewByStatus();
                    case "4" -> add();
                    case "5" -> edit();
                    case "6" -> changeStatus();
                    case "7" -> delete();
                    case "0" -> running = false;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (SQLIntegrityConstraintViolationException e) {
                System.out.println("Error: this computer is referenced by other records (e.g. sessions) "
                        + "and cannot be removed. Set it to Maintenance instead.");
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private void printList(List<Computers> list) {
        if (list.isEmpty()) {
            System.out.println("No computers found.");
            return;
        }
        String[][] data = list.stream()
                .map(c -> new String[]{String.valueOf(c.getId()), c.getComputerName(), c.getStatus()})
                .toArray(String[][]::new);
        UiHelper.table("Computers", new String[]{"ID", "Name", "Status"}, data);
    }

    private void findById() throws SQLException {
        int id = readInt("Computer ID: ");
        Computers c = controller.getById(id);
        printList(List.of(c));
    }

    private void viewByStatus() throws SQLException {
        String status = readStatus("Status");
        printList(controller.getByStatus(status));
    }

    private void add() throws SQLException {
        System.out.print("Computer name: ");
        String name = scanner.nextLine();
        String status = readStatus("Initial status");

        Computers created = controller.add(name, status);
        System.out.println("Added: " + created.getComputerName() + " (ID " + created.getId() + ")");
    }

    private void edit() throws SQLException {
        int id = readInt("Computer ID to edit: ");
        Computers current = controller.getById(id);

        System.out.print("New name [" + current.getComputerName() + "] (Enter to keep): ");
        String name = scanner.nextLine();
        if (name.isBlank()) {
            name = current.getComputerName();
        }

        System.out.print("Change status? Current is " + current.getStatus() + " (y/N): ");
        String status = current.getStatus();
        if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
            status = readStatus("New status");
        }

        controller.update(id, name, status);
        System.out.println("Computer updated.");
    }

    private void changeStatus() throws SQLException {
        int id = readInt("Computer ID: ");
        Computers current = controller.getById(id);
        System.out.println(current.getComputerName() + " is currently " + current.getStatus() + ".");

        String status = readStatus("New status");
        controller.changeStatus(id, status);
        System.out.println("Status updated.");
    }

    private void delete() throws SQLException {
        int id = readInt("Computer ID to delete: ");
        Computers current = controller.getById(id);

        System.out.print("Delete " + current.getComputerName() + "? (Y/N): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
            controller.delete(id);
            System.out.println("Computer deleted.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    // ---------- input helpers ----------

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private String readStatus(String label) {
        while (true) {
            System.out.println(label + ":");
            for (int i = 0; i < STATUSES.length; i++) {
                System.out.println("  " + (i + 1) + ". " + STATUSES[i]);
            }
            int pick = readInt("Choose 1-" + STATUSES.length + ": ");
            if (pick >= 1 && pick <= STATUSES.length) {
                return STATUSES[pick - 1];
            }
            System.out.println("Invalid choice.");
        }
    }
    private void clearScreen() {
        UiHelper.clearScreen();
    }

}
