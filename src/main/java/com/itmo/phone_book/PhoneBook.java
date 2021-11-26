package com.itmo.phone_book;

import com.itmo.phone_book.server.Server;
import com.itmo.phone_book.server.StorageProxy;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class PhoneBook {
    private static final String HELP_STRING = """
            Команды:
            find [keyword]
            add
            update <id>
            quit
            help
            """;
    private final Storage storage;

    public PhoneBook(Storage storage) {
        this.storage = storage;
    }

    public static void main(String[] args) throws IOException {
        final Configuration config = new Configuration(args);
        final Storage storage = createStorage(config);
        PhoneBook phoneBook = new PhoneBook(storage);
        if (config.getMode() == RunMode.SERVER) {
            new Server(storage, config.getPort()).start();
        }

        phoneBook.start();
    }

    public void start() {
        help();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String command = scanner.nextLine();
            if (isBlank(command)) {
                System.err.println("Команды не указана");
                help();

                continue;
            }

            String[] commandArgs = command.split("\s");
            switch (commandArgs[0]) {
                case "help":
                    help();
                    break;
                case "add":
                    add(scanner);
                    break;
                case "update":
                    update(scanner, Integer.parseInt(commandArgs[1]));
                    break;
                case "find":
                    final String keyword = commandArgs.length > 1
                            ? commandArgs[1]
                            : "";
                    find(keyword);
                    break;
                case "quit":
                    System.out.println("Bye");
                    return;
                default:
                    System.err.println("Неизвестная команда: " + commandArgs[0]);
                    help();
                    break;
            }
        }
    }

    private void find(String keyword) {
        final List<Contact> contacts = keyword.isBlank()
                ? storage.find()
                : storage.find(keyword);

        System.out.println("Найдены контакты");
        contacts.forEach(System.out::println);
    }

    private void update(Scanner scanner, int id) {
        final Optional<Contact> contactOpt = storage.find(id);
        contactOpt.ifPresentOrElse(
                contact -> {
                    fillContact(scanner, contact);
                    Contact savedContact = storage.save(contact);

                    System.out.println("Изменён контакт:");
                    System.out.println(savedContact);
                },
                () -> System.err.println("Contact with id " + id + " not found")
        );
    }

    private void add(Scanner scanner) {
        Contact newContact = createDefaultContact();
        fillContact(scanner, newContact);

        Contact savedContact = storage.save(newContact);
        System.out.println("Добавлен контакт:");
        System.out.println(savedContact);
    }

    private void fillContact(Scanner scanner, Contact newContact) {
        System.out.println("Name [" + newContact.getName() +"]:");
        String name = scanner.nextLine();
        if (!isBlank(name)) {
            newContact.setName(name);
        }

        System.out.println("Address [" + newContact.getAddress() +"]:");
        String address = scanner.nextLine();
        if (!isBlank(address)) {
            newContact.setAddress(address);
        }

        System.out.println("Phones [" + newContact.getPhones() +"]:");
        String phones = scanner.nextLine();
        if (!isBlank(phones)) {
            newContact.setPhones(phones);
        }
    }

    private void help() {
        System.out.println(HELP_STRING);
    }
    
    private boolean isBlank(String string) {
        return string == null || string.isBlank();
    }
    
    private Contact createDefaultContact() {
        return new Contact(0, "Anonymous", "Missed address", "666");
    }

    private static Storage createStorage(Configuration config) throws IOException {
        return switch (config.getMode()) {
            case LOCAL -> new InMemoryStorage();
            case SERVER -> new SynchronizedStorage(new InMemoryStorage());
            case CLIENT -> {
                var storage = new StorageProxy(config.getHost(), config.getPort());
                storage.connect();
                yield storage;
            }
        };
    }

    private static class Configuration {
        private final RunMode mode;
        private int port;
        private String host;

        public Configuration(String[] args) {
            this.mode = RunMode.fromArgs(args);
            if (mode == RunMode.SERVER) {
                port = Integer.parseInt(args[1]);
            } else if (mode == RunMode.CLIENT) {
                final String[] split = args[1].split(":");
                host = split[0];
                port = Integer.parseInt(split[1]);
            }
        }

        public RunMode getMode() {
            return mode;
        }

        public int getPort() {
            return port;
        }

        public String getHost() {
            return host;
        }
    }

    private enum RunMode {
        LOCAL,
        SERVER,
        CLIENT;

        public static RunMode fromArgs(String[] args) {
            if (args.length == 0 || "local".equals(args[0])) {
                return LOCAL;
            } else if (args.length > 1) {
                return switch (args[0]) {
                    case "server" -> SERVER;
                    case "client" -> CLIENT;
                    default -> throw new IllegalArgumentException("Unknown mode: " + args[0]);
                };
            }

            throw new IllegalArgumentException("Should not happen");
        }
    }
}
