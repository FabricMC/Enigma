package cuchaz.enigma.config;

import java.io.IOException;

public interface Config {
    String getLanguage();
    void setLanguage(String language);
    void saveConfig() throws IOException;
}
