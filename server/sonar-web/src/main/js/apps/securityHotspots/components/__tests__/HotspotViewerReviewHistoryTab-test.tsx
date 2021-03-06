/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import { shallow } from 'enzyme';
import * as React from 'react';
import {
  mockHotspot,
  mockHotspotReviewHistoryElement
} from '../../../../helpers/mocks/security-hotspots';
import { mockUser } from '../../../../helpers/testMocks';
import { ReviewHistoryType } from '../../../../types/security-hotspots';
import HotspotViewerReviewHistoryTab, {
  HotspotViewerReviewHistoryTabProps
} from '../HotspotViewerReviewHistoryTab';
import HotspotViewerReviewHistoryTabCommentBox from '../HotspotViewerReviewHistoryTabCommentBox';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should propagate onHotspotUpdate correctly', () => {
  const onUpdateHotspot = jest.fn();
  const wrapper = shallowRender({ onUpdateHotspot });

  wrapper
    .find(HotspotViewerReviewHistoryTabCommentBox)
    .props()
    .onUpdateHotspot();
  expect(onUpdateHotspot).toHaveBeenCalled();
});

function shallowRender(props?: Partial<HotspotViewerReviewHistoryTabProps>) {
  return shallow<HotspotViewerReviewHistoryTabProps>(
    <HotspotViewerReviewHistoryTab
      history={[
        mockHotspotReviewHistoryElement({ user: mockUser({ avatar: 'with-avatar' }) }),
        mockHotspotReviewHistoryElement({ user: mockUser({ active: false }) }),
        mockHotspotReviewHistoryElement({ user: mockUser({ login: undefined, name: undefined }) }),
        mockHotspotReviewHistoryElement({
          type: ReviewHistoryType.Diff,
          diffs: [
            { key: 'test', oldValue: 'old', newValue: 'new' },
            { key: 'test-1', oldValue: 'old-1', newValue: 'new-1' }
          ]
        }),
        mockHotspotReviewHistoryElement({
          type: ReviewHistoryType.Comment,
          html: '<strong>bold text</strong>'
        })
      ]}
      hotspot={mockHotspot()}
      onUpdateHotspot={jest.fn()}
      {...props}
    />
  );
}
