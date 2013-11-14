/**
 * 
 */
package org.irods.jargon.rest.commands.collection;

import javax.inject.Named;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.CollectionAndDataObjectListAndSearchAO;
import org.irods.jargon.core.pub.domain.Collection;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;
import org.irods.jargon.rest.commands.AbstractIrodsService;
import org.irods.jargon.rest.domain.CollectionData;
import org.irods.jargon.rest.domain.FileListingEntry;
import org.jboss.resteasy.annotations.providers.jaxb.json.Mapped;
import org.jboss.resteasy.annotations.providers.jaxb.json.XmlNsMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Services for accessing iRODS Collections
 * 
 * @author Mike Conway - DICE (www.irods.org)
 * 
 */
@Named
@Path("/collection")
public class CollectionService extends AbstractIrodsService {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * Retreive information about a collection, and optionally return a listing
	 * of data within the collection as xml or json.
	 * 
	 * @param authorization
	 *            <code>String</code> with the basic auth header
	 * @param offset
	 *            <code>int</code> with an optional (default = 0) offset for any
	 *            listing
	 * @param isListing
	 *            <code>boolean</code> with an optional (default=false)
	 *            parameter that will cause a listing of collection children
	 * @return {@link CollectionData} marshaled in the appropriate format.
	 * @throws JargonException
	 */
	@GET
	@Path("{path:.*}")
	@Produces({ "application/xml", "application/json" })
	@Mapped(namespaceMap = { @XmlNsMap(namespace = "http://irods.org/irods-rest", jsonName = "irods-rest") })
	public CollectionData getCollectionData(
			@HeaderParam("Authorization") final String authorization,
			@PathParam("path") final String path,
			@QueryParam("offset") @DefaultValue("0") final int offset,
			@QueryParam("listing") @DefaultValue("false") final boolean isListing)
			throws JargonException {

		log.info("getCollectionData()");

		if (authorization == null || authorization.isEmpty()) {
			throw new IllegalArgumentException("null or empty authorization");
		}

		if (path == null || path.isEmpty()) {
			throw new IllegalArgumentException("null or empty path");
		}

		try {
			IRODSAccount irodsAccount = retrieveIrodsAccountFromAuthentication(authorization);
			CollectionAO collectionAO = getIrodsAccessObjectFactory()
					.getCollectionAO(irodsAccount);
			// log.info("looking up collection with URI:{}", uri);

			StringBuilder sBuilder = new StringBuilder();
			sBuilder.append('/');
			sBuilder.append(path);

			Collection collection = collectionAO.findByAbsolutePath(sBuilder
					.toString());

			log.info("found collection, marshall the data:{}", collection);
			CollectionData collectionData = new CollectionData();
			collectionData.setCollectionId(collection.getCollectionId());
			collectionData.setCollectionInheritance(collection
					.getCollectionInheritance());
			collectionData.setCollectionMapId(collection.getCollectionMapId());
			collectionData.setCollectionName(collection.getCollectionName());
			collectionData.setCollectionOwnerName(collection
					.getCollectionOwnerName());
			collectionData.setCollectionOwnerZone(collection
					.getCollectionOwnerZone());
			collectionData.setCollectionParentName(collection
					.getCollectionParentName());
			collectionData.setComments(collection.getComments());
			collectionData.setCreatedAt(collection.getCreatedAt());
			collectionData.setInfo1(collection.getInfo1());
			collectionData.setInfo2(collection.getInfo2());
			collectionData.setObjectPath(collection.getObjectPath());
			collectionData.setModifiedAt(collection.getModifiedAt());
			collectionData.setSpecColType(collection.getSpecColType());
			log.info("collectionData:{}", collectionData);

			// if listing, then get children based on given offset
			if (isListing) {
				log.info("add listing with offset at:{}", offset);
				CollectionAndDataObjectListAndSearchAO collectionAndDataObjectListAndSearchAO = getIrodsAccessObjectFactory()
						.getCollectionAndDataObjectListAndSearchAO(irodsAccount);
				FileListingEntry fileListingEntry;

				for (CollectionAndDataObjectListingEntry entry : collectionAndDataObjectListAndSearchAO
						.listCollectionsUnderPath(collection.getAbsolutePath(),
								offset)) {
					fileListingEntry = new FileListingEntry();
					fileListingEntry.setCount(entry.getCount());
					fileListingEntry.setCreatedAt(entry.getCreatedAt());
					fileListingEntry.setDataSize(entry.getDataSize());
					fileListingEntry.setId(entry.getId());
					fileListingEntry.setLastResult(entry.isLastResult());
					fileListingEntry.setModifiedAt(entry.getModifiedAt());
					fileListingEntry.setObjectType(entry.getObjectType());
					fileListingEntry.setOwnerName(entry.getOwnerName());
					fileListingEntry.setOwnerZone(entry.getOwnerZone());
					fileListingEntry.setParentPath(entry.getParentPath());
					fileListingEntry.setPathOrName(entry.getPathOrName());
					fileListingEntry.setSpecColType(entry.getSpecColType());
					fileListingEntry.setSpecialObjectPath(entry
							.getSpecialObjectPath());
					fileListingEntry.setTotalRecords(entry.getTotalRecords());
					collectionData.getChildren().add(fileListingEntry);

				}
				log.info("listing added...");
			}
			return collectionData;
		} finally {
			getIrodsAccessObjectFactory().closeSessionAndEatExceptions();
		}
	}

}