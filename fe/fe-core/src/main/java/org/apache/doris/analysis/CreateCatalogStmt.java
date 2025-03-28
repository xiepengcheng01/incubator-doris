// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.analysis;

import org.apache.doris.catalog.Catalog;
import org.apache.doris.common.AnalysisException;
import org.apache.doris.common.Config;
import org.apache.doris.common.ErrorCode;
import org.apache.doris.common.ErrorReport;
import org.apache.doris.common.FeNameFormat;
import org.apache.doris.common.UserException;
import org.apache.doris.common.util.PrintableMap;
import org.apache.doris.datasource.InternalDataSource;
import org.apache.doris.mysql.privilege.PrivPredicate;
import org.apache.doris.qe.ConnectContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Statement for create a new catalog.
 */
public class CreateCatalogStmt extends DdlStmt {
    private final boolean ifNotExists;
    private final String catalogName;
    private final Map<String, String> properties;

    /**
     * Statement for create a new catalog.
     */
    public CreateCatalogStmt(boolean ifNotExists, String catalogName, Map<String, String> properties) {
        this.ifNotExists = ifNotExists;
        this.catalogName = catalogName;
        this.properties = properties == null ? new HashMap<>() : properties;
    }

    public String getCatalogName() {
        return catalogName;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public boolean isSetIfNotExists() {
        return ifNotExists;
    }

    @Override
    public void analyze(Analyzer analyzer) throws UserException {
        super.analyze(analyzer);
        if (!Config.enable_multi_catalog) {
            throw new AnalysisException("The multi-catalog feature is still in experiment, and you can enable it "
                    + "manually by set fe configuration named `enable_multi_catalog` to be ture.");
        }
        if (catalogName.equals(InternalDataSource.INTERNAL_DS_NAME)) {
            throw new AnalysisException("Internal catalog name can't be create.");
        }
        FeNameFormat.checkCommonName("catalog", catalogName);

        if (!Catalog.getCurrentCatalog().getAuth().checkGlobalPriv(ConnectContext.get(), PrivPredicate.CREATE)) {
            ErrorReport.reportAnalysisException(ErrorCode.ERR_DBACCESS_DENIED_ERROR,
                    analyzer.getQualifiedUser(), catalogName);
        }
        FeNameFormat.checkCatalogProperties(properties);
    }

    @Override
    public String toString() {
        return toSql();
    }

    @Override
    public String toSql() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CREATE CATALOG ").append("`").append(catalogName).append("`");
        if (properties.size() > 0) {
            stringBuilder.append("\nPROPERTIES (\n");
            stringBuilder.append(new PrintableMap<>(properties, "=", true, true, false));
            stringBuilder.append("\n)");
        }
        return stringBuilder.toString();
    }
}
