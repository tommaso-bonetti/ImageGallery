package it.polimi.tiw.imagegallery.beans;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Album {
	private int id;
	private String title;
	private Date creationDate;
	private int ownerId;
	private String ownerUsername;
	
	private static final DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public Date getCreationDate() {
		return creationDate;
	}
	
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	
	public int getOwnerId() {
		return ownerId;
	}
	
	public void setOwnerId(int ownerId) {
		this.ownerId = ownerId;
	}

	public String getOwnerUsername() {
		return ownerUsername;
	}

	public void setOwnerUsername(String ownerUsername) {
		this.ownerUsername = ownerUsername;
	}

	public String getFormattedDate() {
		return formatter.format(creationDate);
	}
}
