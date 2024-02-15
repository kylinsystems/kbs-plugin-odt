/**********************************************************************
 * This file is part of iDempiere ERP Open Source                      *
 * http://www.idempiere.org                                            *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 *                                                                     *
 * Contributors:                                                       *
 * - ken.longnan@gmail.com                                             *
 **********************************************************************/

package com.kylinsystems.kbs.odt;

import java.io.IOException;

import java.io.InputStream;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.adempiere.plugin.utils.AdempiereActivator;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.SystemIDs;
import org.compiere.model.X_AD_Column;
import org.compiere.model.X_AD_SysConfig;
import org.compiere.model.X_AD_TreeNodeMM;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.kylinsystems.kbs.odt.model.MKSODTPackage;
import com.kylinsystems.kbs.odt.model.Utils;
import com.kylinsystems.kbs.odt.model.X_KS_ODTObjectData;
import com.kylinsystems.kbs.odt.model.X_KS_ODTObjectDataLine;
import com.kylinsystems.kbs.odt.model.X_KS_ODTPackage;
import com.kylinsystems.kbs.odt.model.X_KS_ODTVersion;

public class Activator extends AdempiereActivator {

	private static final String XMLTag_ODTPackage 		 = "ODTPackage";
	private static final String XMLTag_ODTVersion		 = "ODTVersion";
	private static final String XMLTag_ODTPackageName    = "Name";
	private static final String XMLTag_ODTObjectData 	 = "ODTObjectData";
	private static final String XMLTag_ODTObjectDataLine = "ODTObjectDataLine";
	

	private boolean isEmpty(String node) {
		if (node != null && !"".equals(node)) {
			return false;
		} else {
			return true;
		}
	}
	
	@Override
	protected void packIn() {
		// Dont packIn 2Pack.zip
	}

	@Override
	protected void install() {
		URL configURL = getContext().getBundle().getEntry("META-INF/ODTPackage.xml");
		if (configURL != null) {
			InputStream input = null;
			try {
				input = configURL.openStream();

				logger.log(Level.INFO, "Loading OPTPackage from bundle:" + getName());
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setNamespaceAware(true);
				dbf.setIgnoringElementContentWhitespace(true);

				DocumentBuilder builder = dbf.newDocumentBuilder();
				InputSource is1 = new InputSource(input);
				Document doc = builder.parse(is1);

				NodeList migrations = doc.getDocumentElement().getElementsByTagName(XMLTag_ODTPackage);
				for ( int j = 0; j < migrations.getLength(); j++ )
				{
					Properties ctx = Env.getCtx();
					Env.setContext(ctx, Env.AD_CLIENT_ID, 0);
					Env.setContext(ctx, Env.AD_USER_ID, SystemIDs.USER_SYSTEM);
					Env.setContext(ctx, Env.AD_ROLE_ID, SystemIDs.ROLE_SYSTEM);

					Element eODTPackage = (Element)migrations.item(j);
					Element eODTVersion = (Element)eODTPackage.getElementsByTagName(XMLTag_ODTVersion).item(0);

					String packageName = ((Element)eODTPackage.getElementsByTagName(XMLTag_ODTPackageName).item(0)).getTextContent().trim();
					int versionNo = Integer.valueOf(eODTVersion.getAttribute(X_KS_ODTVersion.COLUMNNAME_VersionNo));
					
					X_KS_ODTPackage odtPkg = null;
					X_KS_ODTVersion odtVer = null;
					int oldVersionNo = 0;

					// validate package version
					try {
						Query q_pkg = new Query(ctx, X_KS_ODTPackage.Table_Name, "Name='" + packageName + "'", null);
						odtPkg = q_pkg.firstOnly();
												
						Query q_ver = new Query(ctx, X_KS_ODTVersion.Table_Name, "KS_ODTPackage_ID=" + odtPkg.get_ID(), null);
						q_ver.setOrderBy(X_KS_ODTVersion.COLUMNNAME_VersionNo + " DESC ");
						odtVer = q_ver.firstOnly();
					} catch (Exception ex) {
						logger.log(Level.WARNING, "ODT Package Not Existed.", ex.getMessage());
					}
					
					if (odtVer != null)
					{
						oldVersionNo = odtVer.getVersionNo();
					}

					if (versionNo < oldVersionNo)
					{
						logger.log(Level.WARNING, "The version is invalid!"
								+ "(New VersionNo:" + versionNo + " < " + "Old VersionNo:" + oldVersionNo + ")");
						return;
					} else if (versionNo == oldVersionNo)
					{
						logger.log(Level.WARNING, "The version has been already installed!"
								+ "(New VersionNo:" + versionNo + " = " + "Old VersionNo:" + oldVersionNo + ")");
						return;
					} else 
					{
						logger.log(Level.INFO, "Install new version for ODT Package:" + packageName 
								+ "(New VersionNo:" + versionNo + " | " + "Old VersionNo:" + oldVersionNo + ")");
					}

					if (versionNo > oldVersionNo && oldVersionNo != 0)
					{
						// @TODO: uninstall old version
						logger.log(Level.INFO, "Uninstall old version...@TODO@");
					}


					// start installation ODT
					NodeList childrenOD = eODTVersion.getElementsByTagName(XMLTag_ODTObjectData);

					//int Syn_AD_Column_ID = 0;
					for (int i = 0; i < childrenOD.getLength(); i++)
					{
						Element eODTOD = (Element)childrenOD.item(i);
						String AD_Table_ID = eODTOD.getAttribute(X_KS_ODTObjectData.COLUMNNAME_AD_Table_ID);
						String ObjectData_UUID = eODTOD.getAttribute(X_KS_ODTObjectData.COLUMNNAME_ObjectData_UUID);

						// generate PO
						PO po = null;

						MTable table = MTable.get(Env.getCtx(),Integer.valueOf(AD_Table_ID));

//						if ("AD_TreeNodeMM".equalsIgnoreCase(table.getTableName())) {
//							//po = Utils.generatePO2AD_TreeNodeMM(uuid, table);
//							continue;
//						} else {
//							po = Utils.generatePO(ObjectData_UUID, table);
//						}

						po = Utils.generatePO(ObjectData_UUID, table);

						// generate PO value
						NodeList childrenODL = eODTOD.getElementsByTagName(XMLTag_ODTObjectDataLine);
						for (int ii = 0; ii < childrenODL.getLength(); ii++)
						{
							Element eODTODL = (Element)childrenODL.item(ii);
							String AD_Column_ID = eODTODL.getAttribute(X_KS_ODTObjectDataLine.COLUMNNAME_AD_Column_ID);
							Node NewIDNode = (Element)eODTODL.getElementsByTagName(X_KS_ODTObjectDataLine.COLUMNNAME_NewID).item(0);
							Integer NewID = isEmpty(NewIDNode.getTextContent()) == true ? null:Integer.valueOf(NewIDNode.getTextContent());
							Node NewValueNode = (Element)eODTODL.getElementsByTagName(X_KS_ODTObjectDataLine.COLUMNNAME_NewValue).item(0);
							String NewValue = isEmpty(NewValueNode.getTextContent()) == true ? null:NewValueNode.getTextContent();
							Node IsNewNullValueNode = (Element)eODTODL.getElementsByTagName(X_KS_ODTObjectDataLine.COLUMNNAME_IsNewNullValue).item(0);
							Boolean IsNewNullValue = isEmpty(IsNewNullValueNode.getTextContent()) == true ? null:Boolean.valueOf(IsNewNullValueNode.getTextContent());
							Node NewUUIDNode = (Element)eODTODL.getElementsByTagName(X_KS_ODTObjectDataLine.COLUMNNAME_NewUUID).item(0);
							String NewUUID = isEmpty(NewUUIDNode.getTextContent()) == true ? null:NewUUIDNode.getTextContent();

							try {
								po = Utils.buildPO(po, Integer.valueOf(AD_Column_ID), NewID,
										NewValue, IsNewNullValue, NewUUID, table, ctx, logger);
							} catch (Exception ex) {
								logger.log(Level.SEVERE, "Failed to build PO", ex);
								continue;
							}
						}

						if (po instanceof X_AD_TreeNodeMM && po.is_new())
						{
							// reload PO and reset value
							X_AD_TreeNodeMM poMM = (X_AD_TreeNodeMM)po;
							String whereClause_TNMM = "AD_Tree_ID=" + poMM.getAD_Tree_ID() + " AND Node_ID=" + poMM.getNode_ID();
							X_AD_TreeNodeMM reloadPO = (X_AD_TreeNodeMM)table.getPO(whereClause_TNMM, null);
							reloadPO.setSeqNo(poMM.getSeqNo());
							reloadPO.setParent_ID(poMM.getParent_ID());
							reloadPO.set_ValueNoCheck(reloadPO.getUUIDColumnName(), ObjectData_UUID);
							po = reloadPO;
						}
						
						try {
							po.saveEx();
						} catch (Exception poex) {
							logger.log(Level.SEVERE, "Can't save PO!" + "|AD_Table:" + table.getName() + "|ObjectData_UUID:" + ObjectData_UUID);
							throw poex;
						}

						if (po instanceof X_AD_Column)
						{
							X_AD_Column mcol = (X_AD_Column)po;
							if (!mcol.getAD_Table().isView()) {
								Utils.doSynchronizeColumn(mcol.getAD_Column_ID(), ctx);
							}
						}
						
						// apply SQL
						Node SQL_ApplyNode = (Element)eODTOD.getElementsByTagName(X_KS_ODTObjectData.COLUMNNAME_SQL_Apply).item(0);
						String SQL_Apply = isEmpty(SQL_ApplyNode.getTextContent()) == true ? null:SQL_ApplyNode.getTextContent().trim();
						if (SQL_Apply != null && !"".equals(SQL_Apply)) {
							logger.fine("Apply SQL :" + SQL_Apply);
							String[] sqls = SQL_Apply.split("--//--");
							
							int count = 0;
							PreparedStatement pstmt = null;
							for (String sql : sqls) {
								if (sql != null && !"".equals(sql)) {
									try {
										pstmt = DB.prepareStatement(sql, null);
										Statement stmt = null;
										try {
											stmt = pstmt.getConnection().createStatement();
											count = stmt.executeUpdate (sql);
											// if (logger.isLoggable(Level.INFO)) logger.info("Executed SQL Statement for PostgreSQL: "+ sql + " ReturnValue="+count);
										} finally {
											DB.close(stmt);
											stmt = null;
										}
									} catch (Exception e) {
										logger.log(Level.SEVERE,"SQLStatement Error", e);
									} finally {
										DB.close(pstmt);
										pstmt = null;
									}
								}
							}
						}
					} // end of all ODT OD

					Utils.postInstallPackage(ctx);

					// Import into ODTPackage table
					MKSODTPackage.importFromXmlNode(ctx, eODTPackage);
				}

				logger.log(Level.INFO, "Finished install OPTPackage from bundle:" + getName());
			} catch (Exception ex) {
				logger.log(Level.SEVERE, ex.getMessage());
			} finally {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
