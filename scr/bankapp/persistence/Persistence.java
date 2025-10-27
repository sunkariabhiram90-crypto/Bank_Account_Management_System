package bankapp.persistence;

import bankapp.service.BankService;
import java.io.IOException;

public interface Persistence {
    void save(BankService state, String filename) throws IOException;

    Object load(String filename) throws IOException, ClassNotFoundException;
}
