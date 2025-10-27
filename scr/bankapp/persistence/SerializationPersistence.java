package bankapp.persistence;

import bankapp.service.BankService;

import java.io.*;

public class SerializationPersistence implements Persistence {
    @Override
    public void save(BankService state, String filename) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(state);
        }
    }

    @Override
    public Object load(String filename) throws IOException, ClassNotFoundException {
        File f = new File(filename);
        if (!f.exists())
            return null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            return ois.readObject();
        }
    }
}
