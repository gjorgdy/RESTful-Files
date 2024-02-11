package eu.hexasis.files;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping
public class FileController {

	String rootUploadDir = "files";

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
			// Create the uploads directory if not exists
			File uploadDirectory = new File(uploadDir);
			if (!uploadDirectory.exists()) {
				uploadDirectory.mkdirs();
			}

			String[] fileNameSegments = file.getOriginalFilename().split("[.]");
			if (fileNameSegments.length < 1) {
				throw new IOException("Filename is invalid");
			}
			String filetype = fileNameSegments[fileNameSegments.length - 1];
			String filenameBase = file.getOriginalFilename().replace("." + filetype, "");

			System.out.println("File type : " + filetype);
			String filename = random.equals("true")
				  ? getRandomFileName(uploadDir, fileNameSegments[fileNameSegments.length - 1])
				  : getValidName(uploadDir, filenameBase, filetype);
			System.out.println(filename);

			// Save the file to the uploads directory
			String filePath = uploadDir + "/" + filename;
			Files.copy(file.getInputStream(), Path.of(filePath), StandardCopyOption.REPLACE_EXISTING);
			//file.transferTo(new File(filePath));

			response.put("message", "File uploaded successfully!");
			response.put("url", "https://beun.hexasis.eu/files/" + directory + "/" + filename);

		} catch (IOException e) {
			response.put("error", "Error uploading the file: " + e.getMessage());
		}

		return response;
	}

	@GetMapping("/files/{directory}/{filename}")
	public ResponseEntity<InputStreamResource> serveFile(@PathVariable String directory, @PathVariable String filename) {
		System.out.println("Requested file : " + filename);

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

	public static String getValidName(String directory, String basename, String filetype) {
		String fileName = basename
		  .toLowerCase()
		  .replaceAll("(?<!^)\\s(?!$)", "-")
		  .replaceAll("[^a-zA-Z-+\\d]", "")
		  + "." + filetype;
		if (!new File(directory + "/" + fileName).isFile()) {
			return fileName;
		} else {
			int amount;
			Matcher matcher = Pattern.compile("[+]\\d+$").matcher(basename);
			if (matcher.find()) {
				String duplicate = matcher.group();
				amount = Integer.parseInt(duplicate) + 1;
			} else {
				amount = 1;
			}
			return getValidName(directory, basename.replace("[+]\\d+$", "") + "+" + amount, filetype);
		}
	}

	public static String getRandomFileName(String directory, String fileType) {
		UUID uuid = UUID.randomUUID();
		String path = uuid + "." + fileType;
		if (!new File(directory + "/" + path).isFile()) {
			return path;
		} else {
			return getRandomFileName(directory, fileType);
		}
	}

}
