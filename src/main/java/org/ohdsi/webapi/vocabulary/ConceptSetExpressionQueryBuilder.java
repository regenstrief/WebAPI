/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ohdsi.webapi.vocabulary;

import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.ohdsi.webapi.helper.ResourceHelper;

/**
 *
 * Unit tests for CocneptSetExpressionQueryBuilder.
 */
public class ConceptSetExpressionQueryBuilder {

  private final static String CONCEPT_SET_QUERY_TEMPLATE = ResourceHelper.GetResourceAsString("/resources/vocabulary/sql/conceptSetExpression.sql");
  private final static String CONCEPT_SET_EXCLUDE_TEMPLATE = ResourceHelper.GetResourceAsString("/resources/vocabulary/sql/conceptSetExclude.sql");
  private final static String CONCEPT_SET_DESCENDANTS_TEMPLATE = ResourceHelper.GetResourceAsString("/resources/vocabulary/sql/conceptSetDescendants.sql");
  private final static String CONCEPT_SET_MAPPED_TEMPLATE = ResourceHelper.GetResourceAsString("/resources/vocabulary/sql/conceptSetMapped.sql");


  private ArrayList<Long> getConceptIds(ArrayList<Concept> concepts)
  {
    ArrayList<Long> conceptIdList = new ArrayList<>();
    for (Concept concept : concepts) {
      conceptIdList.add(concept.conceptId);
    }
    return conceptIdList;     
  }
  
  private String buildConceptSetQuery(
          ArrayList<Concept> concepts,
          ArrayList<Concept> descendantConcepts,
          ArrayList<Concept> excludeConcepts,
          ArrayList<Concept> excludeDescendantConcepts)
  {
    String conceptSetQuery = StringUtils.replace(CONCEPT_SET_QUERY_TEMPLATE, "@conceptIds",StringUtils.join(getConceptIds(concepts), ","));
    if (descendantConcepts.size() > 0) {
      String includeDescendantQuery = StringUtils.replace(CONCEPT_SET_DESCENDANTS_TEMPLATE, "@conceptIds", StringUtils.join(getConceptIds(descendantConcepts), ","));
      conceptSetQuery = StringUtils.replace(conceptSetQuery,"@descendantQuery", includeDescendantQuery);
    } else {
      conceptSetQuery = StringUtils.replace(conceptSetQuery, "@descendantQuery", "");
    }
    if (excludeConcepts.size() > 0)
    {
      String excludeClause = StringUtils.replace(CONCEPT_SET_EXCLUDE_TEMPLATE,"@conceptIds", StringUtils.join(getConceptIds(excludeConcepts),","));
      if (excludeDescendantConcepts.size() > 0){
        String excludeClauseDescendantQuery = StringUtils.replace(CONCEPT_SET_DESCENDANTS_TEMPLATE, "@conceptIds", StringUtils.join(getConceptIds(excludeDescendantConcepts), ","));
        excludeClause = StringUtils.replace(excludeClause, "@descendantQuery", excludeClauseDescendantQuery);
      } else {
        excludeClause = StringUtils.replace(excludeClause, "@descendantQuery", "");
      }
      conceptSetQuery += excludeClause;
    }
    
    return conceptSetQuery;
  }
  
  public String buildExpressionQuery(ConceptSetExpression expression)
  {
    // handle included concepts.
    ArrayList<Concept> includeConcepts = new ArrayList<>();
    ArrayList<Concept> includeDescendantConcepts = new ArrayList<>();
    ArrayList<Concept> excludeConcepts = new ArrayList<>();
    ArrayList<Concept> excludeDescendantConcepts = new ArrayList<>();
    
    ArrayList<Concept> includeMappedConcepts = new ArrayList<>();
    ArrayList<Concept> includeMappedDescendantConcepts = new ArrayList<>();
    ArrayList<Concept> excludeMappedConcepts = new ArrayList<>();
    ArrayList<Concept> excludeMappedDescendantConcepts = new ArrayList<>();
    
    // populate each sub-set of cocnepts from the flags set in each concept set item
    for (ConceptSetExpression.ConceptSetItem item : expression.items)
    {
      if (!item.isExcluded)
      {
        includeConcepts.add(item.concept);

        if (item.includeDescendants)
          includeDescendantConcepts.add(item.concept);

        if (item.includeMapped)
        {
          includeMappedConcepts.add(item.concept);
          if (item.includeDescendants)
            includeMappedDescendantConcepts.add(item.concept);
        }
      } else {
        excludeConcepts.add(item.concept);
        if (item.includeDescendants)
          excludeDescendantConcepts.add(item.concept);
        if (item.includeMapped)
        {
          excludeMappedConcepts.add(item.concept);
          if (item.includeDescendants)
            excludeMappedDescendantConcepts.add(item.concept);
        }
      }
    }
    
    // each ArrayList contains the concepts that are used in the sub-query of the codeset expression query
    
    // sanity check: if there are no included concepts, throw exception
    if (includeConcepts.isEmpty())
      throw new RuntimeException("Codeset Expression contained zero included concepts.  A codeset expression must contain at least 1 concept that is not excluded.");
    
    String conceptSetQuery = buildConceptSetQuery(includeConcepts, includeDescendantConcepts, excludeConcepts, excludeDescendantConcepts);
    
    if (includeMappedConcepts.size() > 0){
      String mappedConceptsQuery = buildConceptSetQuery(includeMappedConcepts, includeMappedDescendantConcepts, excludeMappedConcepts, excludeMappedDescendantConcepts);
      conceptSetQuery += "\nUNION\n" + StringUtils.replace(CONCEPT_SET_MAPPED_TEMPLATE, "@conceptsetQuery", mappedConceptsQuery);
    }
    
    return conceptSetQuery;
  }
}
