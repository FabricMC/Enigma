package cuchaz.enigma.gui.filechooser;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.Library;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NativeFileDialog;

import java.io.File;
import java.nio.ByteBuffer;

public class NativeDirectoryChooser {

	static {
		Library.initialize();
	}

	File selectedFile;

	public File show(){
		PointerBuffer outPath = MemoryUtil.memAllocPointer(1);
		NativeFileDialog.NFD_PickFolder((ByteBuffer) null, outPath);
		String path = outPath.getStringUTF8();
		if(path == null){
			return null;
		}
		File file = new File(path);
		if(file.isDirectory()){
			selectedFile = file;
			return file;
		}
		return null;
	}

	public File getSelectedFile() {
		return selectedFile;
	}

	public void setSelectedFile(File selectedFile) {
		this.selectedFile = selectedFile;
	}
}
