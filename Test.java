import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

interface IWarehouse {
    void loadProducts(String filename);

    void fulfillOrders(String filename);

    boolean mergeTotes();

    void displayDetails();
}

class Product {
    private String upc;

    public Product(String upc) {
        this.upc = upc;
    }

    public String getUpc() {
        return upc;
    }
}

class Tote {
    private List<Product> products;

    public Tote() {
        this.products = new ArrayList<>();
    }

    public List<Product> getProducts() {
        return products;
    }

    public void addProduct(Product product) {
        products.add(product);
    }

    public boolean isFull() {
        return products.size() >= 10;
    }

    public boolean isEmpty() {
        return products.isEmpty();
    }

    @Override
    public String toString() {
        return "Tote{" +
                "products=" + products +
                '}';
    }
}

class Warehouse implements IWarehouse {
    private List<Tote> totes;

    public Warehouse() {
        this.totes = new ArrayList<>();
    }

    public void loadProducts(String filename) {
        try (Scanner scanner = new Scanner(new File(filename))) {
            System.out.println("Loading products...");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(", ");
                String upc = parts[0];
                Product product = new Product(upc);
                placeProductInTote(product);
            }
            System.out.println("Loading complete.");
        } catch (FileNotFoundException e) {
            System.err.println("Error: Product file not found.");
        }
    }

    public void fulfillOrders(String filename) {
        try (Scanner scanner = new Scanner(new File(filename))) {
            System.out.println("\nFulfilling orders...");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(", ");
                String orderNum = parts[0];
                List<String> orderProducts = Arrays.asList(Arrays.copyOfRange(parts, 1, parts.length));
                fulfillOrder(orderNum, orderProducts);
            }
            System.out.println("\nOrders complete.");
        } catch (FileNotFoundException e) {
            System.err.println("Error: Order file not found.");
        }
    }

   public boolean mergeTotes() {
    System.out.println("\nMerging partially filled totes...");
    Map<String, List<Tote>> productTotesMap = new HashMap<>();

    // Collect products in each tote
    for (Tote tote : totes) {
        for (Product product : tote.getProducts()) {
            productTotesMap.computeIfAbsent(product.getUpc(), k -> new ArrayList<>()).add(tote);
        }
    }

    // Merge totes for each product
    for (Map.Entry<String, List<Tote>> entry : productTotesMap.entrySet()) {
        List<Tote> totesWithProduct = entry.getValue();
        if (totesWithProduct.size() > 1) {
            // Merge totes
            Tote mergedTote = new Tote();
            for (Tote tote : totesWithProduct) {
                mergedTote.getProducts().addAll(tote.getProducts());
            }

            // Remove old totes
            totes.removeAll(totesWithProduct);

            // Add merged tote
            totes.add(mergedTote);
        }
    }

    // Remove empty totes
    totes.removeIf(Tote::isEmpty);

    System.out.println("Merge complete.");
    return true;
}

    public void displayDetails() {
        System.out.println("\nWarehouse details:");
        int fullTotes = 0;
        int partiallyFilledTotes = 0;
        int emptyTotes = 0;

        System.out.printf("%-15s %s%n", "UPC", "# Totes");
        System.out.println("--------------- -------");
        for (Tote tote : totes) {
            String upc = tote.isEmpty() ? "Empty" : tote.getProducts().get(0).getUpc();
            int toteCount = Collections.frequency(totes, tote);
            System.out.printf("%-15s %d%n", upc, toteCount);

            if (tote.isFull()) {
                fullTotes++;
            } else if (!tote.isEmpty()) {
                partiallyFilledTotes++;
            } else {
                emptyTotes++;
            }
        }

        System.out.println("\nWarehouse summary:");
        System.out.println(fullTotes + " full totes, " + partiallyFilledTotes + " partially filled totes, and " + emptyTotes + " empty totes");
    }

    private void placeProductInTote(Product product) {
        boolean productPlaced = false;
        for (Tote tote : totes) {
            if (!tote.isFull() && tote.getProducts().get(0).getUpc().equals(product.getUpc())) {
                tote.addProduct(product);
                productPlaced = true;
                break;
            }
        }
        if (!productPlaced) {
            Tote newTote = new Tote();
            newTote.addProduct(product);
            totes.add(newTote);
            System.out.println("Used additional tote (" + (totes.size() - 1) + ") for UPC: " + product.getUpc());
        }
    }

    private void fulfillOrder(String orderNum, List<String> orderProducts) {
        System.out.println("\nOrder fulfillment started: Order " + orderNum);
        for (String upc : orderProducts) {
            Optional<Tote> tote = findToteWithProduct(upc);
            if (tote.isPresent()) {
                System.out.println("Retrieving product from tote (" + totes.indexOf(tote.get()) + ") for UPC: " + upc);
                tote.get().getProducts().removeIf(product -> product.getUpc().equals(upc));
                System.out.println("Order fulfilled--> Order " + orderNum + ", " + upc);
            } else {
                System.out.println("Product with UPC " + upc + " not found in any tote.");
            }
        }
    }

    private Optional<Tote> findToteWithProduct(String upc) {
        List<Tote> totesWithProduct = new ArrayList<>();
        for (Tote tote : totes) {
            if (!tote.isEmpty() && tote.getProducts().get(0).getUpc().equals(upc)) {
                totesWithProduct.add(tote);
            }
        }
        if (!totesWithProduct.isEmpty()) {
            return Optional.of(totesWithProduct.get(new Random().nextInt(totesWithProduct.size())));
        } else {
            return Optional.empty();
        }
    }
}

public class Test {
    public static void main(String[] args) {
        IWarehouse w = new Warehouse();

        w.loadProducts("products.txt");
        w.displayDetails();

        w.fulfillOrders("orders.txt");
        w.displayDetails();

        w.mergeTotes();
        w.displayDetails();
    }
}
