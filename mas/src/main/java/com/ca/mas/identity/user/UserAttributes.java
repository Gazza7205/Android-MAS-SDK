/*
 * Copyright (c) 2016 CA. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 */

package com.ca.mas.identity.user;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.ca.mas.identity.common.Attribute;
import com.ca.mas.identity.common.Attributes;
import com.ca.mas.identity.util.IdentityConsts;
import com.ca.mas.identity.util.IdentityUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * <p><b>UserAttributes</b> is the specialization of the {@link Attributes} class and deals specifically with SCIM users. UserAttributes is a
 * {@link com.ca.mas.foundation.MASTransformable} implementer that is able to populate itself.</p>
 */
public class UserAttributes extends Attributes {

    /**
     * <b>Description:</b> No args constructor.
     */
    public UserAttributes() {
        super();
    }

    /**
     * <b>Description:</b> Convenience constructor.
     *
     * @param attributes the list of user attributes supported by this SCIM service.
     */
    public UserAttributes(List<String> attributes) {
        super(attributes);
    }

    @Override
    public void populate(@NonNull JSONObject jobj) throws JSONException {
        List<Attribute> mUserAttributes = new ArrayList<>();

        // save the schema for future reference
        IdentityUtil.SCHEMA_MAP.put(IdentityConsts.KEY_USER_ATTRIBUTES, jobj);

        // get the attributes
        JSONArray jsonAttribs = jobj.optJSONArray(IdentityConsts.KEY_ATTRIBUTES);
        for (int attrIndex = 0; attrIndex < jsonAttribs.length(); attrIndex++) {
            JSONObject attribObj = jsonAttribs.getJSONObject(attrIndex);
            if (attribObj == null) {
                continue;
            }

            // Attribute meta-data
            Attribute attribute = new Attribute();
            attribute.populate(attribObj);
            mUserAttributes.add(attribute);

            // 1. Each name is a potential attribute that the implementer can use as a filter.
            String name = attribObj.optString(IdentityConsts.KEY_NAME);
            if (!TextUtils.isEmpty(name)) {
                // SUBATTRIBUTES
                // 2. If the name is non-null, we need to check for if it has subAttributes.
                // This is because we only want the attrib.subAttrib form of this attribute. For
                // example, if there is name.familyName, name.givenName, then 'name' without a qualifier
                // has no meaning.
                JSONArray subAttribs = attribObj.optJSONArray(IdentityConsts.KEY_SUB_ATTRIBUTES);
                if (subAttribs != null) {
                    for (int subAttrIndex = 0; subAttrIndex < subAttribs.length(); subAttrIndex++) {
                        JSONObject subAttrib = subAttribs.optJSONObject(subAttrIndex);
                        if (subAttrib != null) {
                            // subattributes meta-data
                            Attribute subAttribute = new Attribute();
                            subAttribute.populate(subAttrib);
                            mUserAttributes.add(subAttribute);

                            String subName = subAttrib.optString(IdentityConsts.KEY_NAME);
                            if (!TextUtils.isEmpty(subName)) {
                                mAttributes.add(name + "." + subName);
                            }
                        }
                    }
                } else {
                    mAttributes.add(name);
                }
            }
        }
    }

    @Override
    protected String getKey() {
        return IdentityConsts.KEY_USER_ATTRIBUTES;
    }
}
