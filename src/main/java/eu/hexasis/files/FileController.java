package eu.hexasis.files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping
public class FileController {

	String rootUploadDir = "files";

	// creating a logger
	Logger logger = LoggerFactory.getLogger(FileController.class);

	@PostMapping("/api")
	@ResponseBody
	public Map<String, String> handleFileUpload(
			@RequestParam("file") MultipartFile file,
			@RequestParam("directory") String directory,
			@RequestParam("random") String random
	) {

		Map<String, String> response = new HashMap<>();

		String uploadDir = rootUploadDir + "/" + directory;

		try {
			// create the upload directory if it does not exist
			File uploadDirectory = new File(uploadDir);
			if (!uploadDirectory.exists()) {
				uploadDirectory.mkdirs();
			}
			// get name of file
			String fileName = file.getOriginalFilename();
			if (fileName == null) {
				throw new IOException("Filename is invalid");
			}
			// deconstruct name
			String fileNameBase = null;
			String fileTypeSuffix = null;
			for (int i = fileName.length() - 1 ; i > 0 ; i--) {
				if (fileName.charAt(i) == '.') {
					fileNameBase = fileName.substring(0, i);
					fileTypeSuffix = fileName.substring(i);
					break;
				}
			}
			if (fileNameBase == null) {
				throw new IOException("Filename is invalid");
			}
			// reconstruct into valid name
			String filename = random.equals("true")
				  ? getRandomFileName(uploadDir, fileTypeSuffix)
				  : getValidName(uploadDir, fileNameBase, fileTypeSuffix);
			// save the file to the uploads directory
			String filePath = path(uploadDir, filename);
			Files.copy(file.getInputStream(), Path.of(filePath), StandardCopyOption.REPLACE_EXISTING);
			// return message
			response.put("message", "File uploaded successfully!");
			response.put("url", path(RestFiles.BASE_URL, rootUploadDir, directory, filename));

		} catch (IOException e) {
			response.put("error", "Error uploading the file: " + e.getMessage());
		}
		return response;
	}

	@GetMapping("/error")
	public ResponseEntity<InputStreamResource> error() {
		return ResponseEntity.status(404)
		   .body(new InputStreamResource(new ByteArrayInputStream("Nuh uh".getBytes())));
	}

	@GetMapping("/files/")
	public ResponseEntity<InputStreamResource> serveFile() {

		File folder = new File(rootUploadDir);
		StringBuilder stringBuilder = new StringBuilder();
		if (folder.isDirectory()) {
			File[] subfolders = folder.listFiles();
			if (subfolders == null) return null;
			for (File file : subfolders) {
				String name = file.getName();
				stringBuilder
					.append("<a href=\"/files/")
					.append(name)
					.append("\">")
					.append(name)
					.append("</a>")
					.append("<br>");
			}
		}

		InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(stringBuilder.toString().getBytes()));

		return ResponseEntity.ok()
					   .body(resource);
	}

	@GetMapping("/files/{directory}/{filename}/")
	public ResponseEntity<InputStreamResource> serveFileSlash(@PathVariable String directory, @PathVariable String filename) {
		logger.info("Requested file : " + filename);

		try {
			File file = new File(rootUploadDir + "/" + directory + "/" + filename);
			InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

			return ResponseEntity.ok()
			   .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
			   .contentType(MediaType.APPLICATION_OCTET_STREAM)
			   .contentLength(file.length())
			   .body(resource);
		} catch (IOException e) {
			return ResponseEntity.ok().build();
		}
	}

	@GetMapping("/files/{directory}/{filename}")
	public ResponseEntity<InputStreamResource> serveFile(@PathVariable String directory, @PathVariable String filename) {
		return serveFileSlash(directory, filename);
	}

	public static String getValidName(String directory, String basename, String fileTypeSuffix) {
		String fileNameFlat = basename
		  .toLowerCase()
		  .replaceAll("(?<!^)\\s(?!$)", "-")
		  .replaceAll("[^a-zA-Z-+\\d]", "");
		for (int i = 0 ; i < 10000 ; i++) {
			String filename = fileNameFlat + disc(i) + fileTypeSuffix;
			if (!new File(path(directory, filename)).isFile()) return filename;
		}
		return getRandomFileName(directory, fileTypeSuffix);
	}

	public static String disc(int i) {
		return i == 0 ? "" : "+" + i;
	}

	public static String getRandomFileName(String directory, String fileTypeSuffix) {
		UUID uuid = UUID.randomUUID();
		String fileName = uuid + fileTypeSuffix;
		if (!new File(path(directory, fileName)).isFile()) {
			return fileName;
		} else {
			return getRandomFileName(directory, fileTypeSuffix);
		}
	}

	public static String path(String... args) {
		StringBuilder r = new StringBuilder();
		int l = args.length;
		for (int i = 0 ; i < l ; i++) {
			r.append(args[i]);
			if (i != l - 1) {
				r.append("/");
			}
		}
		return r.toString();
	}

}
