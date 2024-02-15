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

package com.kylinsystems.kbs.odt.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Trx;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.kylinsystems.kbs.odt.model.MKSODTPackage;
import com.kylinsystems.kbs.odt.model.Utils;

@org.adempiere.base.annotation.Process
public class ImportPackageProcess extends SvrProcess {

	private String fileName = null;
	private boolean apply = false;

	@Override
	protected String doIt() throws Exception {

		File file = new File(fileName);
		if ( !file.exists() )
			throw new AdempiereException("@FileNotFound@");

		File[] migrationFiles = null;
		if ( file.isDirectory() )
		{
			migrationFiles = file.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {

					return name.endsWith(".xml");
				}
			});
		}
		else
		{
			migrationFiles = new File[] {file};
		}

		for (File xmlfile : migrationFiles )
		{
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Loading ODTPackage from file: " + file.getAbsolutePath());
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			dbf.setIgnoringElementContentWhitespace(true);

			DocumentBuilder builder = dbf.newDocumentBuilder();
			InputSource is1 = new InputSource(new FileInputStream(xmlfile));
			Document doc = builder.parse(is1);

			NodeList migrations = doc.getDocumentElement().getElementsByTagName("ODTPackage");
			for ( int i = 0; i < migrations.getLength(); i++ )
			{
				MKSODTPackage migration = MKSODTPackage.importFromXmlNode(getCtx(), (Element) migrations.item(i));
				if ( apply && migration != null)
				{
					migration.apply();
				} else if (migration == null)
				{
					return "ODTPackage is invalid!";
				}
			}
		}

		return "Import complete";

	}

	@Override
	protected void prepare() {
		ProcessInfoParameter[] paras = getParameter();
		for ( ProcessInfoParameter para : paras )
		{
			if ( para.getParameterName().equals("FileName"))
				fileName = (String) para.getParameter();
			else if ( para.getParameterName().equals("Apply"))
				apply  = para.getParameterAsBoolean();
		}
	}

}
