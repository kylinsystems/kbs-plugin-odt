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

import java.util.logging.Level;

import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereSystemError;

import com.kylinsystems.kbs.odt.model.MKSODTPackage;

@org.adempiere.base.annotation.Process
public class ExportPackageProcess extends SvrProcess
{
	private int p_ODTPackage_ID = 0;

	private MKSODTPackage odtPackage;

	@Override
	protected void prepare()
	{
		p_ODTPackage_ID = getRecord_ID();
	}

	@Override
	protected String doIt() throws Exception
	{
		if (p_ODTPackage_ID == 0)
			throw new AdempiereSystemError("@NotFound@ @KS_ODTPackage_ID@ " + p_ODTPackage_ID);
		if (log.isLoggable(Level.INFO)) log.info("doIt - KS_ODTPackage_ID=" + p_ODTPackage_ID);

		try {
			odtPackage = new MKSODTPackage(getCtx(), p_ODTPackage_ID, get_TrxName());
			odtPackage.exportPackage();
		}
		catch (Exception e)
		{
			if (log.isLoggable(Level.SEVERE)) log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			throw e;
		}

		return "#" + "ODT Package Exported Successful!";
	}
}
