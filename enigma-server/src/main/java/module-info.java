module enigma.server {
    exports cuchaz.enigma.network;
    exports cuchaz.enigma.network.packet;

    requires cuchaz.enigma;
    requires com.google.common;
    requires joptsimple;
    requires java.desktop;
}